package com.logviewer.tests.utils;

import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;

public class TestLogFormats {
    /**
     * Files under `log-viewer-cli/src/test/resources/integration/data/multifile`
     */
    public static final LogFormat MULTIFILE = new LogbackLogFormat("%date{yyMMdd HH:mm:ss} %msg%n");

    /**
     * log-viewer-cli/src/test/resources/integration/data/search.log
     */
    public static final LogFormat SEARCH = new LogbackLogFormat("[%d{yyyy.MM.dd HH:mm}]%message%n");

    /**
     * log-viewer-cli/src/test/resources/integration/data/level-log4j.log
     */
    public static final LogFormat FORMAT_LEVEL_LOG4j = new Log4jLogFormat("%d{yyMMdd HH:mm:ss} %p %m%n");
    public static final LogFormat FORMAT_LEVEL_LOGBACK = new LogbackLogFormat("%d{yyMMdd HH:mm:ss} %p %m%n");

    public static final LogFormat SIMPLE_FORMAT = new LogbackLogFormat("%d{yyyy-MM-dd HH:mm:ss} %m%n");

}
