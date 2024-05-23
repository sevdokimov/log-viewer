package com.logviewer.springboot;

import com.logviewer.config.LogViewerAutoConfig;
import com.logviewer.config.LvConfigBase;
import com.logviewer.web.LogViewerServlet;
import com.logviewer.web.LogViewerServletJakarta;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.stream.Stream;

@Import({LogViewerAutoConfig.class, LvConfigBase.class})
@Configuration
public class LogViewerSpringBootConfig {

    public static final String LOG_VIEWER_WEBSOCKET_PATH = "log-viewer.websocket.path";

    public static final String DEFAULT_LOG_PATH = "/logs/*";

    public static final String LOG_VIEWER_URL_MAPPING = "log-viewer.url-mapping";

    @Bean
    public ServletRegistrationBean logViewerServlet(Environment environment) {
        ServletRegistrationBean servletReg = new ServletRegistrationBean();
        servletReg.setName("logViewerServlet");
        servletReg.setAsyncSupported(true);

        // Log-viewer supports both "javax.servlet.*" and "jakarta.servlet.*"

        Object servletInstance;
        if (JakartaSupport.useJakarta(getClass().getClassLoader())) {
            servletInstance = new LogViewerServletJakarta();
        } else {
            servletInstance = new LogViewerServlet();
        }

        Method setServlet = Stream.of(ServletRegistrationBean.class.getMethods()).filter(m -> m.getName().equals("setServlet")).findFirst().get();
        ReflectionUtils.invokeMethod(setServlet, servletReg, servletInstance);

        String logServletPath = environment.getProperty(LOG_VIEWER_URL_MAPPING, DEFAULT_LOG_PATH);
        if (!logServletPath.endsWith("*")) {
            if (!logServletPath.endsWith("/"))
                logServletPath += "/";

            logServletPath += "*";
        }

        servletReg.setUrlMappings(Collections.singletonList(logServletPath));

        String websocketPath = environment.getProperty(LOG_VIEWER_WEBSOCKET_PATH);
        if (websocketPath != null && !websocketPath.isEmpty()) {
            servletReg.addInitParameter("web-socket-path", websocketPath);
        }

        return servletReg;
    }
}
