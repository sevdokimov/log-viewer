package com.logviewer.data2;

import com.typesafe.config.Config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LogLevels {
    // See org.apache.log4j.lf5.LogLevel, org.apache.log4j.Level, https://en.wikipedia.org/wiki/Syslog
    private static final String[] LEVELS = {
            "OFF", "FATAL", "SEVERE", "EMERGENCY", "ALERT", "CRITICAL", "ERROR",
            "WARN", "WARNING",
            "INFO", "CONFIG", "NOTICE", "INFORMATIONAL",
            "DEBUG", "FINE", "FINER",
            "FINEST", "TRACE", "ALL",
    };

    private static String[] CUSTOM_LEVELS = new String[0];

    public LogLevels(Config config) {
        if (config.hasPath("log-viewer.log-levels")) {
            setCustomLevels(config.getStringList("log-viewer.log-levels"));
        }
    }

    public void setCustomLevels(List<String> levels) {
        LogLevels.CUSTOM_LEVELS = levels.toArray(new String[0]);
    }

    public static String[] getLevels() {
        return Stream.concat(
                Stream.of(LEVELS),
                Stream.of(CUSTOM_LEVELS)
        ).distinct().toArray(String[]::new);
    }
}
