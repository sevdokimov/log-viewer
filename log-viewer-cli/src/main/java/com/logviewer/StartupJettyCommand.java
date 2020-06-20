package com.logviewer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.logviewer.config.LvServerConfig;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.web.LogViewerServlet;
import com.logviewer.web.LogViewerWebsocket;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.websocket.server.ServerEndpointConfig;
import java.lang.management.ManagementFactory;
import java.net.URL;

public class StartupJettyCommand extends CommandHandler<StartupJettyCommand.CliStartupArgs> {

    private static final Logger LOG = LoggerFactory.getLogger(StartupJettyCommand.class);

    public static final int DEFAULT_PORT = 8111;

    protected StartupJettyCommand() {
        super(new CliStartupArgs());
    }

    @Override
    public String getCommandName() {
        return "startup";
    }

    private static Server server;

    public static void startup(CliStartupArgs params) throws Exception {
        boolean closeAppContext = false;

        ApplicationContext appCtx = LogContextHolder.getInstance();
        if (appCtx == null) {
            appCtx = new AnnotationConfigApplicationContext(LvJettyConfig.class, LvServerConfig.class);
            LogContextHolder.setInstance(appCtx);
            closeAppContext = true;
        }

        if (params.isDisableUi())
            return;

        try {
            Server srv = new Server(params.getPort());

            WebAppContext webAppCtx = new WebAppContext();

            webAppCtx.setContextPath("/");

            String webXmlStr = LogViewerMain.class.getClassLoader().getResource("log-viewer-web/WEB-INF/web.xml").toString();
            URL webAppUrl = new URL(webXmlStr.substring(0, webXmlStr.length() - "WEB-INF/web.xml".length()));

            webAppCtx.setBaseResource(Resource.newResource(webAppUrl));
            webAppCtx.setAttribute(LogViewerServlet.SPRING_CONTEXT_PROPERTY, appCtx);

            srv.setHandler(webAppCtx);

            ServletHolder lvServlet = webAppCtx.addServlet(LogViewerServlet.class, "/*");
            if (params.isNoWs()) {
                lvServlet.setAsyncSupported(true);
            } else {
                ServerContainer websocketCtx = WebSocketServerContainerInitializer.configureContext(webAppCtx);
                websocketCtx.addEndpoint(ServerEndpointConfig.Builder.create(LogViewerWebsocket.class, "/ws").build());

                lvServlet.setInitParameter("web-socket-path", "ws");
            }

            srv.start();

            server = srv;
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

    @Override
    public void execute() throws Exception {
        startup(argsHolder);

        if (!argsHolder.isDisableUi()) {
            LOG.info("Web interface started: http://localhost:{} ({}ms)",
                    argsHolder.getPort(), ManagementFactory.getRuntimeMXBean().getUptime());
        } else {
            synchronized (this) {
                this.wait(); // Sleep forever.
            }
        }
    }

    /**
     *
     */
    @Parameters(commandDescription = "Start web interface")
    public static class CliStartupArgs {
        @Parameter(description = "Port to bind web interface", names = {"-p", "--port"})
        private int port = DEFAULT_PORT;

        @Parameter(description = "Disable using websocket, communication will be performed using POST requests", names = {"-nws", "--no-websocket"})
        private boolean noWs;

        @Parameter(description = "Disable web UI", names = {"-nui", "--no-web-ui"})
        private boolean disableUi;

        public int getPort() {
            return port;
        }

        public CliStartupArgs setPort(int port) {
            this.port = port;
            return this;
        }

        public boolean isNoWs() {
            return noWs;
        }

        public CliStartupArgs setNoWs(boolean noWs) {
            this.noWs = noWs;
            return this;
        }

        public boolean isDisableUi() {
            return disableUi;
        }

        public CliStartupArgs setDisableUi(boolean disableUi) {
            this.disableUi = disableUi;
            return this;
        }
    }
}
