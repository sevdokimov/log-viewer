package com.logviewer.config;

import com.logviewer.data2.net.server.LogViewerBackdoorServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogViewerServerConfig {

    @Bean
    public LogViewerBackdoorServer lvLogServer() {
        return new LogViewerBackdoorServer();
    }

}
