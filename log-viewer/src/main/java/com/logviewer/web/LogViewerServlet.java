package com.logviewer.web;

import com.logviewer.data2.LogContextHolder;
import com.logviewer.utils.Utils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public class LogViewerServlet extends HttpServlet {

    public static final String SPRING_CONTEXT_PROPERTY = "org.springframework.web.context.WebApplicationContext.ROOT";

    private static final Pattern[] RESOURCE_PATTERNS = new Pattern[]{
            Pattern.compile("/fa-(?:brands|regular|solid)-\\d+(\\.[0-9a-f]{20})?\\.[a-z0-9]+"),
            Pattern.compile("/MaterialIcons-Regular(\\.[0-9a-f]{20})?\\.[a-z0-9]+"),
            Pattern.compile("/[\\w\\-]+(\\.[0-9a-f]{20})?\\.(?:png|gif)"),
            Pattern.compile("/(?:main|polyfills|runtime|styles|vendor)(\\.[0-9a-f]{20})?\\.(?:css|js|js\\.map|css\\.map)"),
    };

    private static final Map<String, String> MIME_TYPES_MAP = Utils.newMap("css", "text/css", "js", "application/javascript");

    private volatile String indexHtml;

    private final Map<String, ResourceCache> resourceCache = new ConcurrentHashMap<>();

    private Map<String, AbstractRestRequestHandler> restHandlers;

    @Override
    public void init() {
        ApplicationContext logContext = getSpringContext();

        restHandlers = Utils.newMap(
                "navigator", injectDeps(logContext, new LogNavigatorController()),
                "ws-emulator", injectDeps(logContext, new WebsocketEmulationController()),
                "log-view", injectDeps(logContext, new LogViewController())
        );
    }

    private static <T> T injectDeps(ApplicationContext logContext, T controller) {
        logContext.getAutowireCapableBeanFactory().autowireBeanProperties(controller, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
        return controller;
    }

    protected ApplicationContext getSpringContext() {
        ApplicationContext appCtx = LogContextHolder.getInstance();
        if (appCtx != null)
            return appCtx;

        appCtx = (ApplicationContext) getServletConfig().getServletContext().getAttribute(SPRING_CONTEXT_PROPERTY);
        if (appCtx != null)
            return appCtx;
        
        throw new IllegalStateException("Spring context not found. Set ApplicationContext to " +
                "com.logviewer.data2.LogContextHolder.setInstance(appCtx)");
    }

    private String getRelativePath(HttpServletRequest req) {
        String servletMappingPath = req.getContextPath() + req.getServletPath();

        assert req.getRequestURI().startsWith(servletMappingPath) : "[req.getRequestURI()=" + req.getRequestURI()
                + ", req.getContextPath()=" + req.getContextPath() + ", req.getServletPath()=" + req.getServletPath();

        String uri = req.getRequestURI().substring(servletMappingPath.length());
        assert uri.startsWith("/") || uri.isEmpty();

        return uri;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String relativePath = getRelativePath(req);

        String restPrefix = "/rest/";

        if (!relativePath.startsWith(restPrefix)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        int idx = relativePath.indexOf("/", restPrefix.length());
        if (idx < 0) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AbstractRestRequestHandler handler = restHandlers.get(relativePath.substring(restPrefix.length(), idx));
        if (handler == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        handler.process(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String relativePath = getRelativePath(req);

        if (relativePath.equals("") || relativePath.equals("/")) {
            processIndexHtml(req, resp, relativePath);
            return;
        }

        if (relativePath.startsWith("/rest/")) {
            int idx = relativePath.indexOf("/", "/rest/".length());
            if (idx < 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            AbstractRestRequestHandler handler = restHandlers.get(relativePath.substring("/rest/".length(), idx));
            if (handler == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            handler.process(req, resp);

            return;
        }

        Boolean cachableRes = null;
        if (relativePath.startsWith("/assets/") || relativePath.startsWith("/img/")) {
            cachableRes = true;
        } else {
            for (Pattern resourcePattern : RESOURCE_PATTERNS) {
                Matcher matcher = resourcePattern.matcher(relativePath);
                if (matcher.matches()) {
                    cachableRes = matcher.group(1) != null;
                    break;
                }
            }
        }

        if (cachableRes == null) {
            processIndexHtml(req, resp, relativePath);
            return;
        }

        ResourceCache cache;

        try {
            cache = this.resourceCache.computeIfAbsent(relativePath, key -> {
                URL resource = getClass().getResource("/log-viewer-web/" + key);
                if (resource == null)
                    throw new IllegalArgumentException();

                ResourceCache res = new ResourceCache();
                try {
                    res.url = resource;
                    res.contentType = detectMimeType(key);

                    if ("jar".equals(resource.getProtocol())) {
                        URLConnection urlConnection = resource.openConnection();

                        res.date = urlConnection.getLastModified();
                        res.length = urlConnection.getContentLengthLong();

                        if (res.length < 5 * 1024) {
                            try (InputStream inputStream = urlConnection.getInputStream()) {
                                res.data = StreamUtils.copyToByteArray(inputStream);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return res;
            });
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (cachableRes) {
            resp.setHeader("Cache-Control", "max-age=31536000");
            resp.setHeader("Pragma", "cache");
            resp.setDateHeader("Expires", System.currentTimeMillis() + 31536000000L);
        }

        long ifModifiedSince = req.getDateHeader("If-Modified-Since");

        if (ifModifiedSince != -1 && cachableRes) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        URLConnection urlConnection = null;

        long date = cache.date;
        if (date == -1) {
            urlConnection = cache.url.openConnection();
            date = urlConnection.getLastModified();
        }

        if (ifModifiedSince != -1 && ifModifiedSince >= date) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        resp.addDateHeader("Last-Modified", date);

        if (cache.contentType != null)
            resp.addHeader("Content-Type", cache.contentType);

        if (cache.data != null) {
            resp.getOutputStream().write(cache.data);
        }
        else {
            if (urlConnection == null)
                urlConnection = cache.url.openConnection();

            long length = cache.length;
            if (length == -1)
                length = urlConnection.getContentLengthLong();

            if (length >= 10*1024) {
                if ((cache.contentType == null
                        || (!cache.contentType.startsWith("image/")) && !relativePath.endsWith(".woff") && !relativePath.endsWith(".woff2"))) {
                    String acceptEncoding = req.getHeader("Accept-Encoding");
                    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                        resp.setHeader("Content-Encoding", "gzip");

                        GZIPOutputStream gzip = new GZIPOutputStream(resp.getOutputStream());

                        try (InputStream inputStream = urlConnection.getInputStream()) {
                            StreamUtils.copy(inputStream, gzip);
                        }

                        gzip.finish();

                        return;
                    }
                }
            }

            try (InputStream inputStream = urlConnection.getInputStream()) {
                StreamUtils.copy(inputStream, resp.getOutputStream());
            }
        }
    }

    @Override
    public void destroy() {
        if (restHandlers != null) {
            for (AbstractRestRequestHandler value : restHandlers.values()) {
                Utils.closeQuietly(value);
            }
        }
    }

    private String calculateRootPath(HttpServletRequest req, String relativePath) {
        if (relativePath.indexOf('/', 1) < 0)
            return "./";

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i < relativePath.length(); i++) {
            if (relativePath.charAt(i) == '/')
                sb.append("../");
        }

        return sb.toString();
    }

    private void processIndexHtml(HttpServletRequest req, HttpServletResponse resp, String relativePath) throws IOException {
        if (relativePath.isEmpty()) {
            String path = req.getContextPath() + req.getServletPath();

            if (!path.isEmpty()) {
                int slash = path.lastIndexOf('/');
                assert slash >= 0 : path;

                String target = "." + path.substring(slash) + "/";
                if (req.getQueryString() != null)
                    target += '?' + req.getQueryString();
                    
                resp.sendRedirect(target);
                return;
            }
        }

        String indexHtml = this.indexHtml;
        if (indexHtml == null) {
            URL indexHtmlUrl = getClass().getResource("/log-viewer-web/index.html");
            if (indexHtmlUrl == null) {
                resp.sendError(500, "index.html not found");
                return;
            }

            try (InputStream stream = indexHtmlUrl.openStream()) {
                indexHtml = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
            }

            if (!indexHtmlUrl.getProtocol().equals("file"))
                this.indexHtml = indexHtml;
        }

        String rootPath = calculateRootPath(req, relativePath);


        indexHtml = indexHtml.replace("$PATH", rootPath);

        indexHtml = indexHtml.replace("$WEB_SOCKET_PATH", getWebSocketPath(rootPath));

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setContentType("text/html");
        resp.getOutputStream().print(indexHtml);
    }

    @NonNull
    private String getWebSocketPath(String rootPath) {
        String webSocketPath = getServletConfig().getInitParameter("web-socket-path");

        if (webSocketPath == null || webSocketPath.isEmpty())
            return "";

        if (webSocketPath.startsWith("/"))
            webSocketPath = webSocketPath.substring(1);

        return rootPath + webSocketPath;
    }

    private static String detectMimeType(String fileName) {
        String res = URLConnection.guessContentTypeFromName(fileName);
        if (res == null) {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex >= 0) {
                res = MIME_TYPES_MAP.get(fileName.substring(dotIndex + 1));
            }
        }

        return res;
    }

    private static class ResourceCache {
        private long date = -1;
        private long length = -1;
        private byte[] data;

        private URL url;
        private String contentType;
    }
}
