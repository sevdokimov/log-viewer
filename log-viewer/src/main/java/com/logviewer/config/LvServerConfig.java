package com.logviewer.config;

import com.logviewer.data2.LogService;
import com.logviewer.data2.net.server.LogServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LvServerConfig {

    @Bean(initMethod = "startup", destroyMethod = "close")
    public LogServer lvLogServer(LogService logService, Environment environment) {
        int port = environment.getProperty("log-viewer.server.port", Integer.class, LogServer.DEFAULT_PORT);
        return new LogServer(logService, port);
    }

}
