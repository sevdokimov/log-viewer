package com.logviewer;

import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.web.LogViewerServlet;
import com.logviewer.web.LogViewerWebsocket;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.websocket.server.ServerEndpointConfig;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;

public class LogViewerMain {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewerMain.class);

    @Value("${log-viewer.server.port:8111}")
    private int port;
    @Value("${log-viewer.server.interface:}")
    private String serverInterface;
    @Value("${log-viewer.server.enabled:true}")
    private boolean enabled;
    @Value("${log-viewer.use-web-socket:true}")
    private boolean useWebSocket;

    private static Server server;

    public boolean startup() throws Exception {
        boolean closeAppContext = false;

        ApplicationContext appCtx = LogContextHolder.getInstance();
        if (appCtx == null) {
            appCtx = new AnnotationConfigApplicationContext(LvStandaloneConfig.class, LogViewerServerConfig.class);
            LogContextHolder.setInstance(appCtx);
            closeAppContext = true;
        }

        appCtx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_NO, false);

        if (!enabled)
            return false;

        try {
            Server srv = new Server();
            ServerConnector connector=new ServerConnector(srv);
            connector.setPort(port);
            if (!serverInterface.isEmpty()) {
                InetAddress e = InetAddress.getByName(serverInterface);
                connector.setHost(e.getHostAddress());
            }
            srv.setConnectors(new Connector[]{connector});

            WebAppContext webAppCtx = new WebAppContext();

            webAppCtx.setContextPath("/");

            String webXmlStr = LogViewerMain.class.getClassLoader().getResource("log-viewer-web/WEB-INF/web.xml").toString();
            URL webAppUrl = new URL(webXmlStr.substring(0, webXmlStr.length() - "WEB-INF/web.xml".length()));

            webAppCtx.setBaseResource(Resource.newResource(webAppUrl));
            webAppCtx.setAttribute(LogViewerServlet.SPRING_CONTEXT_PROPERTY, appCtx);

            srv.setHandler(webAppCtx);

            ServletHolder lvServlet = webAppCtx.addServlet(LogViewerServlet.class, "/*");
            if (useWebSocket) {
                lvServlet.setAsyncSupported(true);
            } else {
                ServerContainer websocketCtx = WebSocketServerContainerInitializer.configureContext(webAppCtx);
                websocketCtx.addEndpoint(ServerEndpointConfig.Builder.create(LogViewerWebsocket.class, "/ws").build());

                lvServlet.setInitParameter("web-socket-path", "ws");
            }

            srv.start();

            server = srv;

            LOG.info("Web interface started: http://localhost:{} ({}ms)", port, ManagementFactory.getRuntimeMXBean().getUptime());

            return true;
        }
        finally {
            if (server == null) {
                if (closeAppContext) {
                    ((ConfigurableApplicationContext)appCtx).close();
                    LogContextHolder.setInstance(null);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        TypesafePropertySourceFactory.getHoconConfig(); // Checks that config is exist and valid.

        LogViewerMain run = new LogViewerMain();

        if (!run.startup()) {
            synchronized (LogViewerMain.class) {
                LogViewerMain.class.wait(); // Jetty was not started. Sleep forever to avoid closing the process.
            }
        }
    }

}
