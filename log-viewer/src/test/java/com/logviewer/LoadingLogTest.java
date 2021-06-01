package com.logviewer;

import com.logviewer.data2.Log;
import com.logviewer.data2.LogRecord;
import com.logviewer.data2.Snapshot;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoadingLogTest extends AbstractLogTest {

    @Test
    public void testFormatModification() throws IOException {
        LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

        String logPath = getTestLog("multilog/search.log");

        Log log = getLogService().openLog(logPath, format);

        format.setPattern("%l");

        assertEquals("%d{ddMMyy HH:mm:ss} %m", ((LogbackLogFormat)log.getFormat()).getPattern());

        try (Snapshot snapshot = log.createSnapshot()) {
            List<LogRecord> list = new ArrayList<>();
            snapshot.processRecords(1, list::add);

            assertEquals("150101 10:00:01 zzz a", list.get(0).getMessage());
        }
    }

}
