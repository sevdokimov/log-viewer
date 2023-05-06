package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.LogRecord;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SimpleLogFormatTest extends AbstractLogTest {

    @Test
    public void testAsciiColorCodes() throws IOException {
        List<LogRecord> records = loadLog("LogParser/ascii-color-codes.log", new SimpleLogFormat());
        assertEquals("2022-01-02_11:00:00 foo", records.get(0).getMessage());
        assertEquals("2022-01-02_15:00:00 bar", records.get(1).getMessage());
        assertEquals(",fff", records.get(2).getMessage());
    }

}