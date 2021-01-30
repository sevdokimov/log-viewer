package com.logviewer.logLibs.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.config.VisibleDirectory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class LogbackConfigImporter implements Supplier<Map<Path, LogFormat>> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogbackConfigImporter.class);

    private static void processAppender(Appender<ILoggingEvent> appender, Set<Appender<ILoggingEvent>> processedAppenders,
                                        ArrayList<VisibleDirectory> visibleDirectories, Map<Path, LogFormat> res) {
        if (!processedAppenders.add(appender))
            return;

        if (appender instanceof FileAppender<?>) {
            FileAppender fileAppender = (FileAppender) appender;

            Encoder encoder = fileAppender.getEncoder();

            if (!(encoder instanceof PatternLayoutEncoder))
                throw new IllegalStateException("Failed to import log config - unsupported encoder: " + encoder.getClass());

            try {
                File file = new File(fileAppender.getFile()).getCanonicalFile();

                File parent = file.getParentFile();
                if (parent == null)
                    return;

                PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) encoder;
                LogFormat logFormat = new LogbackLogFormat(patternEncoder.getCharset(), patternEncoder.getPattern());

                try {
                    logFormat.getFields(); // check errors.
                } catch (IllegalArgumentException e) {
                    LOG.error("Failed to import log configuration, invalid pattern: " + patternEncoder.getPattern(), e);
                    logFormat = null;
                }

                visibleDirectories.add(new VisibleDirectory(parent.getPath(), Pattern.quote(file.getName())));

                res.put(file.toPath(), logFormat);
            } catch (IOException e) {
                LOG.error("Failed to import log configuration", e);
            }
        } else if (appender instanceof AppenderAttachable) {
            Iterator<Appender<ILoggingEvent>> itr = ((AppenderAttachable) appender).iteratorForAppenders();
            while (itr.hasNext()) {
                processAppender(itr.next(), processedAppenders, visibleDirectories, res);
            }
        }
    }

    @Override
    public Map<Path, LogFormat> get() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        ArrayList<VisibleDirectory> visibleDirectories = new ArrayList<>();

        Set<Appender<ILoggingEvent>> processedAppenders = new HashSet<>();

        Map<Path, LogFormat> res = new HashMap<>();

        for (Logger logger : loggerContext.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                processAppender(index.next(), processedAppenders, visibleDirectories, res);
            }
        }

        return res;
    }
}
