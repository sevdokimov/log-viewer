package com.logviewer.springboot;

import com.logviewer.web.LogViewerWebsocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import static com.logviewer.springboot.LogViewerSpringBootConfig.*;

@Configuration
@PropertySource("classpath:log-viewer-springboot.properties")
public class LogViewerWebsocketConfig {

    @Bean
    public ServerEndpointRegistration logViewerWebSocket(Environment environment) {
        String path = environment.getRequiredProperty(LOG_VIEWER_WEBSOCKET_PATH);
        if (!path.startsWith("/"))
            path = '/' + path;

        String logServletPath = environment.getProperty(LOG_VIEWER_URL_MAPPING, DEFAULT_LOG_PATH);

        logServletPath = logServletPath.replaceAll("/+\\**$", "");
        
        return new ServerEndpointRegistration(logServletPath + path, LogViewerWebsocket.class);
    }

    @ConditionalOnMissingBean(ServerEndpointExporter.class)
    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new ServerEndpointExporter();
    }
}
