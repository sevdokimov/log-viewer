package com.logviewer.logLibs.log4j;

import com.logviewer.data2.LogFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Log4jConfigImporter implements Supplier<Map<Path, LogFormat>> {

    @Override
    public Map<Path, LogFormat> get() {
        LoggerContext context = (LoggerContext) LogManager.getContext();

        Map<Path, LogFormat> res = new HashMap<>();

        Configuration configuration = context.getConfiguration();

        for (Appender appender : configuration.getAppenders().values()) {
            Path path;

            if (appender instanceof FileAppender) {
                path = Paths.get(((FileAppender) appender).getFileName());
            } else if (appender instanceof RollingFileAppender) {
                path = Paths.get(((RollingFileAppender) appender).getFileName());
            } else {
                continue;
            }

            Layout<? extends Serializable> layout = appender.getLayout();
            if (layout instanceof PatternLayout) {
                PatternLayout patternLayout = (PatternLayout) layout;

                String conversionPattern = patternLayout.getConversionPattern();

                res.put(path, new Log4jLogFormat(conversionPattern));
            }
        }

        return res;
    }
}
