package com.logviewer;

import com.logviewer.config.LogViewerServerConfig;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.websocket.server.ServerEndpointConfig;
import java.lang.management.ManagementFactory;
import java.net.URL;

public class StartupJettyCommand {

}
