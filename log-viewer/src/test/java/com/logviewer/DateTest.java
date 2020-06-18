package com.logviewer;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogCrashedException;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Record;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.RegexLogFormat.RegexpField;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DateTest extends AbstractLogTest {

    private static LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8, null,
            "\\[(\\d{4}-\\d\\d-\\d\\d_\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d)] \\[(.+?)\\] ([A-Z]+) +((?:[\\w\\$]+\\.)*[\\w\\$]+) - (.*)",
            "yyyy-MM-dd_HH:mm:ss.SSS", "date",
            new RegexpField("date", 1, FieldTypes.DATE), new RegexpField("thread", 2), new RegexpField("level", 3),
            new RegexpField("cls", 4), new RegexpField("msg", 5));

    @Test
    public void logWithTime() throws ParseException, IOException, LogCrashedException {
        List<Record> records = loadLog("date/log-with-time.log", logFormat);

        assertEquals(3, records.size());
        assertEquals("2018-04-08_23:54:00.330", records.get(0).getFieldText(logFormat.getFieldIndexByName("date")));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").parse("2018-04-08_23:54:00.330").getTime(), records.get(0).getTime());
    }
}
