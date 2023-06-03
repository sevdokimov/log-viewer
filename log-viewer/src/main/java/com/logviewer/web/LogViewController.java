package com.logviewer.web;

import com.logviewer.api.LvFilterStorage;
import com.logviewer.api.LvPermalinkStorage;
import com.logviewer.data2.*;
import com.logviewer.data2.net.Node;
import com.logviewer.data2.net.server.LogViewerBackdoorServer;
import com.logviewer.domain.Permalink;
import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Pair;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogViewController extends AbstractRestRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewController.class);

    @Autowired
    private LvPermalinkStorage permalinkStorage;

    @Autowired
    private LvFilterStorage filterStorage;

    @Autowired
    private LogService logService;

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public String generatePermalink(String[] hashAndPermalink) throws IOException {
        String hash = hashAndPermalink[0];
        Permalink permalink = LvGsonUtils.GSON.fromJson(hashAndPermalink[1], Permalink.class);

        if ((permalink.getLogList() == null)
                || permalink.getOffset() == null || permalink.getHashes() == null) {
            throw new IllegalArgumentException();
        }

        return permalinkStorage.save(hash, permalink);
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public void saveFilterState(String[] filterStateAndHash) {
        String hash = filterStateAndHash[0];
        String filterState = filterStateAndHash[1];

        filterStorage.saveFilterSet(hash, filterState);
    }

    @Endpoint(method = {FormSubmitEvent.MethodType.GET, FormSubmitEvent.MethodType.POST})
    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException, ExecutionException {
        String filtersJson = request.getParameter("filters");
        RecordPredicate[] filters;
        if (filtersJson == null || filtersJson.isEmpty()) {
            filters = new RecordPredicate[0];
        } else {
            filters = LvGsonUtils.GSON.fromJson(filtersJson, RecordPredicate[].class);
        }

        String[] files = request.getParameterValues("log");

        if (files == null || files.length == 0)
            throw new RestException(400, "File list is not specified");

        Set<LogPath> paths = new HashSet<>();

        for (String file : files) {
            paths.addAll(LogPath.parsePathFromHttpParameter(file));
        }

        if (paths.isEmpty())
            throw new RestException(400, "No files to download");

        CompletableFuture<Map<String, LogView>> logs = logService.openLogs(paths);

        Map<String, LogView> map;
        try {
            map = logs.get();
        } catch (InterruptedException e) {
            logs.cancel(true);
            throw new RuntimeInterruptedException(e);
        }

        for (LogView logView : map.values()) {
            if (!logView.isConnected())
                throw new RestException(407, "Failed to download log: " + logView.getPath());
        }

        String fileName = request.getParameter("fileName");
        if (fileName == null || fileName.isEmpty())
            throw new RestException(400, "'fileName' parameter is not provided");

        if ("on".equals(request.getParameter("zip")) || map.size() > 1) {
            if (!fileName.toLowerCase().endsWith(".zip"))
                fileName = fileName + ".zip";

            setDisposition(response, fileName);
            response.setContentType("application/zip");

            Map<String, LogView> nameMap = assignUniqueNames(map.values());

            ZipOutputStream zOut = new ZipOutputStream(response.getOutputStream());

            String[] key = nameMap.keySet().toArray(new String[0]);
            Arrays.sort(key);

            for (String name : key) {
                ZipEntry entry = new ZipEntry(name);
                zOut.putNextEntry(entry);

                LogView logView = nameMap.get(name);

                writeLog(logView, zOut, filters);
            }

            zOut.close();
            return;
        }

        setDisposition(response, fileName);
        response.setContentType("text/plain");

        LogView logView = map.values().iterator().next();

        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            response.setHeader("Content-Encoding", "gzip");

            GZIPOutputStream gzip = new GZIPOutputStream(response.getOutputStream());
            writeLog(logView, gzip, filters);
            gzip.finish();
        } else {
            writeLog(logView, response.getOutputStream(), filters);
        }
    }

    private void writeLog(LogView logView, OutputStream out, @Nullable RecordPredicate[] filters) {
        AtomicReference<Status> res = new AtomicReference<>();
        CountDownLatch cnt = new CountDownLatch(1);

        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        Position start = new Position(logView.getId(), 0, 0);
        
        LogProcess logProcess = logView.loadRecords(CompositeRecordPredicate.and(filters), Integer.MAX_VALUE, start,
                false, null, Long.MAX_VALUE, new LogDataListener() {
                    @Override
                    public void onData(@NonNull RecordList data) {
                        if (cnt.getCount() == 0)
                            return;

                        try {
                            for (Pair<LogRecord, Throwable> pair : data) {
                                writer.append(pair.getFirst().getMessage()).append('\n');
                            }
                        } catch (IOException e) {
                            res.set(new Status(e));
                            cnt.countDown();
                        }
                    }

                    @Override
                    public void onFinish(@NonNull Status status) {
                        if (cnt.getCount() == 0)
                            return;

                        res.set(status);
                        cnt.countDown();
                    }
                });

        logProcess.start();

        try {
            cnt.await();
        } catch (InterruptedException e) {
            logProcess.cancel();
            throw new RuntimeInterruptedException(e);
        }

        if (res.get().getError() != null) {
            logProcess.cancel();
            LOG.error("Failed to download log", res.get().getError());
            throw new RestException(500, "Failed to download log: " + res.get().getError());
        }

        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Map<String, LogView> assignUniqueNames(Collection<LogView> logs) {
        Map<String, Set<String>> shortName2path = new HashMap<>();

        for (LogView log : logs) {
            String file = log.getPath().getFile();
            Path path = Paths.get(file);
            Set<String> paths = shortName2path.computeIfAbsent(path.getFileName().toString(), key -> new HashSet<>());
            paths.add(file);
        }

        Map<String, String> path2file = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : shortName2path.entrySet()) {
            Set<String> paths = entry.getValue();
            if (paths.size() == 1) {
                path2file.put(paths.iterator().next(), entry.getKey());
            } else {
                String prefix = null;
                for (String path : paths) {
                    if (prefix == null) {
                        prefix = path;
                    } else {
                        prefix = commonPrefix(prefix, path);
                    }
                }
                assert prefix != null;
                int slashIdx = prefix.lastIndexOf('/');

                for (String path : paths) {
                    path2file.put(path, path.substring(slashIdx + 1));
                }
            }
        }

        boolean hasHost = logs.stream().map(l -> l.getPath().getNode()).distinct().count() > 1;
        boolean hasPort = hasHost && logs.stream()
                .filter(l -> l.getPath().getNode() != null)
                .map(l -> {
                    Integer port = l.getPath().getNode().getPort();
                    return port == null ? LogViewerBackdoorServer.DEFAULT_PORT : port;
                }).distinct().count() > 1;

        Map<String, LogView> res = new HashMap<>();

        for (LogView log : logs) {
            LogPath path = log.getPath();

            String fileName = path2file.get(path.getFile()).replaceAll("[\\\\/*?:&|>]", "_");

            Node node = path.getNode();
            if (node != null && hasHost) {
                if (hasPort && node.getPort() != null) {
                    fileName = node.getHost() + '-' + node.getPort() + "_" + fileName;
                } else {
                    fileName = node.getHost() + "_" + fileName;
                }
            }

            if (res.containsKey(fileName)) {
                if (fileName.toLowerCase().endsWith(".log")) {
                    fileName = fileName.substring(0, fileName.length() - ".log".length()) + "." + log.getId() + ".log";
                } else {
                    fileName = fileName + '-' + log.getId();
                }
            }

            res.put(fileName, log);
        }

        return res;
    }

    private static String commonPrefix(@NonNull String a, @NonNull String b) {
        for (int i = 0, max = Math.min(a.length(), b.length()); i < max; i++) {
            if (a.charAt(i) != b.charAt(i))
                return a.substring(0, i);
        }

        return a.length() < b.length() ? a : b;
    }

    private static void setDisposition(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        String headerVal = String.format("attachment;filename=\"%s\";filename*=UTF-8''%s", fileName, URLEncoder.encode(fileName, "UTF-8"));
        response.setHeader("Content-Disposition", headerVal);
    }
}
