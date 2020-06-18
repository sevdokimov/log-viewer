package com.logviewer.tests.web;

import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;

public class WrongFormatTest extends AbstractWebTestCase {

    private static final LogFormat FORMAT = new LogbackLogFormat("%date{yyMMdd HH:mm:ss.SSS} %msg%n");

    @Test
    public void testWrongFormat() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT);
        openLog("multifile/log-a.log");

        waitFor(() -> getVisibleRecords().endsWith("150101 10:00:03 a 4"));
    }
}
