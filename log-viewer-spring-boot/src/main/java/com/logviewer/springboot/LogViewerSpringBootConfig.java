package com.logviewer.springboot;

import com.logviewer.config.LogViewerAutoConfig;
import com.logviewer.config.LvConfigBase;
import com.logviewer.web.LogViewerServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Collections;

@Import({LogViewerAutoConfig.class, LvConfigBase.class})
@Configuration
public class LogViewerSpringBootConfig {

    public static final String LOG_VIEWER_WEBSOCKET_PATH = "log-viewer.websocket.path";

    @Bean
    public ServletRegistrationBean logViewerServlet(Environment environment) {
        ServletRegistrationBean<LogViewerServlet> servlet = new ServletRegistrationBean<>();
        servlet.setName("logViewerServlet");
        servlet.setAsyncSupported(true);
        servlet.setServlet(new LogViewerServlet());

        String logServletPath = environment.getProperty("log-viewer.url-mapping", "/logs/*");
        if (!logServletPath.endsWith("*")) {
            if (!logServletPath.endsWith("/"))
                logServletPath += "/";

            logServletPath += "/*";
        }

        servlet.setUrlMappings(Collections.singletonList(logServletPath));

        String websocketPath = environment.getProperty(LOG_VIEWER_WEBSOCKET_PATH);
        if (websocketPath != null && !websocketPath.isEmpty()) {
            servlet.addInitParameter("web-socket-path", websocketPath);
        }

        return servlet;
    }
}
