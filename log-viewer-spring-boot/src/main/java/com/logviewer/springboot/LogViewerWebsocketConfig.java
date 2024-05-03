package com.logviewer.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import java.lang.reflect.Constructor;

import static com.logviewer.springboot.LogViewerSpringBootConfig.*;

@Configuration
@ConditionalOnClass(ServerEndpointRegistration.class)
@ConditionalOnProperty(name = "log-viewer.use-web-socket", matchIfMissing = true)
@PropertySource("classpath:log-viewer-springboot.properties")
public class LogViewerWebsocketConfig {

    @Bean
    public ServerEndpointRegistration logViewerWebSocket(Environment environment) {
        String path = environment.getRequiredProperty(LOG_VIEWER_WEBSOCKET_PATH);
        if (!path.startsWith("/"))
            path = '/' + path;

        String logServletPath = environment.getProperty(LOG_VIEWER_URL_MAPPING, DEFAULT_LOG_PATH);

        logServletPath = logServletPath.replaceAll("/+\\**$", "");

        // Log-viewer supports both "javax.servlet.*" and "jakarta.servlet.*"

        Class endpointCls = JakartaSupport.loadJavaxOrJakartaClass(LogViewerWebsocketConfig.class.getClassLoader(),
                "com.logviewer.web.LogViewerWebsocket");

        try {
            Constructor<ServerEndpointRegistration> constructor = ServerEndpointRegistration.class.getConstructor(String.class, Class.class);
            return constructor.newInstance(logServletPath + path, endpointCls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ConditionalOnMissingBean(ServerEndpointExporter.class)
    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new ServerEndpointExporter();
    }
}
