package com.logviewer.logLibs.log4j;

import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.logback.LogbackConfigImporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Log4jConfigImporter implements Supplier<Map<Path, LogFormat>> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogbackConfigImporter.class);

    @Override
    public Map<Path, LogFormat> get() {
        Map<Path, LogFormat> res = new HashMap<>();

        configure(res, LogManager.getContext(true));
        configure(res, LogManager.getContext(false));

        return res;
    }

    private void configure(Map<Path, LogFormat> res, org.apache.logging.log4j.spi.LoggerContext context) {
        if (!(context instanceof LoggerContext))
            return;
            
        Configuration configuration = ((LoggerContext)context).getConfiguration();

        for (Appender appender : configuration.getAppenders().values()) {
            Path path;

            if (appender instanceof FileAppender) {
                path = Paths.get(((FileAppender) appender).getFileName());
            } else if (appender instanceof RollingFileAppender) {
                path = Paths.get(((RollingFileAppender) appender).getFileName());
            } else {
                continue;
            }

            try {
                path = path.toRealPath();
            } catch (IOException e) {
                LOG.error("Failed to get path for log: " + path, e);
                continue;
            }

            Layout<? extends Serializable> layout = appender.getLayout();
            if (layout instanceof PatternLayout) {
                PatternLayout patternLayout = (PatternLayout) layout;

                String conversionPattern = patternLayout.getConversionPattern();

                res.put(path, new Log4jLogFormat(conversionPattern));
            }
        }
    }
}
