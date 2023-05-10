package com.logviewer;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.RegexLogFormat.RegexField;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DateTest extends AbstractLogTest {

    private final static LogFormat logFormat = new RegexLogFormat(
            "\\[(\\d{4}-\\d\\d-\\d\\d_\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d)] \\[(.+?)\\] ([A-Z]+) +((?:[\\w\\$]+\\.)*[\\w\\$]+) - (.*)",
            "yyyy-MM-dd_HH:mm:ss.SSS", "date",
            new RegexField("date", 1, FieldTypes.DATE), new RegexField("thread", 2), new RegexField("level", 3),
            new RegexField("cls", 4), new RegexField("msg", 5));

    @Test
    public void logWithTime() throws ParseException, IOException {
        List<LogRecord> records = loadLog("date/log-with-time.log", logFormat);

        assertEquals(3, records.size());
        assertEquals("2018-04-08_23:54:00.330", records.get(0).getFieldText("date"));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").parse("2018-04-08_23:54:00.330").getTime(), records.get(0).getTimeMillis());
    }
}
