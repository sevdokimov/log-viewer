package com.logviewer.tests.utils;

import com.logviewer.data2.LogFormat;
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

}
