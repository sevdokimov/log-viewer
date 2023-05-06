package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.Log;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import com.logviewer.data2.Snapshot;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RegexLogFormatTest extends AbstractLogTest {

    @Test
    public void testNamedGroupFields() {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>\\d+) (?<msg>.+)", false,
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
                );

        LogRecord record = read(logFormat, "111 message text");

        assertEquals("111", record.getFieldText("date"));
        assertEquals("message text", record.getFieldText("msg"));
    }

    @Test
    public void testDateUTC() throws ParseException {
        LogFormat logFormat = new RegexLogFormat(Locale.US,
                StandardCharsets.UTF_8,
                "(?<date>[^ ]+) (?<msg>.+)", true,
                "yyyy-MM-dd:HH:mm:ssZ", "date",
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
        );

        LogRecord record = read(logFormat, "2020-02-02:01:01:01+0000 message text");

        assertEquals("2020-02-02:01:01:01+0000", record.getFieldText("date"));
        assertEquals("message text", record.getFieldText("msg"));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertEquals(simpleDateFormat.parse("2020-02-02 01:01:01").getTime(), record.getTimeMillis());
    }

    @Test
    public void testDate() {
        LogFormat logFormat = new RegexLogFormat(Locale.US,
                StandardCharsets.UTF_8,
                "(?<date>[^ ]+) (?<msg>.+)", true,
                "yyyy-MM-dd:HH:mm:ss", "date",
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
                );

        LogRecord record = read(logFormat, "2020-02-02:01:01:01 message text");

        assertEquals("2020-02-02:01:01:01", record.getFieldText("date"));
        assertEquals("message text", record.getFieldText("msg"));

        assertEquals(new Date(120, Calendar.FEBRUARY, 2, 1, 1, 1).getTime(), record.getTimeMillis());
    }

    @Test
    public void testDateInOptionalField() {
        LogFormat logFormat = new RegexLogFormat(Locale.US,
                StandardCharsets.UTF_8,
                "(?<level>\\w+) (?<date>\\d[^ ]+)? (?<msg>.+)", true,
                "yyyy-MM-dd:HH:mm:ss", "date",
                RegexLogFormat.field("level", FieldTypes.LEVEL),
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
                );

        LogRecord record = read(logFormat, "DEBUG 2020-02-02:01:01:01 message text");

        assertEquals("2020-02-02:01:01:01", record.getFieldText("date"));
        assertEquals("message text", record.getFieldText("msg"));
        assertEquals(new Date(120, Calendar.FEBRUARY, 2, 1, 1, 1).getTime(), record.getTimeMillis());

        record = read(logFormat, "DEBUG  message text");

        assert record.getTime() <= 0;
        assertEquals("DEBUG", record.getFieldText("level"));
        assertEquals("message text", record.getFieldText("msg"));
    }

    @Test
    public void appendTailDisabled() throws IOException {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>\\d+) (?<msg>.+)", true,
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
        );

        String logPath = getTestLog("LogParser/regex-log-parser.log");
        Log log = getLogService().openLog(logPath, logFormat);

        try (Snapshot snapshot = log.createSnapshot()) {
            List<LogRecord> res = new ArrayList<>();

            snapshot.processRecords(0, res::add);

            assertEquals(Arrays.asList(
                    "111 aaa",
                    "a_ a_ a_\na__ a__ a__",
                    "222 bbb",
                    "b_ b_ b_"
                    ),
                    res.stream().map(LogRecord::getMessage).collect(Collectors.toList()));
        }

    }

    @Test
    public void appendTail() throws IOException {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>\\d+) (?<msg>.+)", false,
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
        );

        String logPath = getTestLog("LogParser/regex-log-parser.log");
        Log log = getLogService().openLog(logPath, logFormat);

        try (Snapshot snapshot = log.createSnapshot()) {
            List<LogRecord> res = new ArrayList<>();

            snapshot.processRecords(0, res::add);

            assertEquals(Arrays.asList(
                    "111 aaa\na_ a_ a_\na__ a__ a__",
                    "222 bbb\nb_ b_ b_"
                    ),
                    res.stream().map(LogRecord::getMessage).collect(Collectors.toList()));
        }

    }

    @Test
    public void testAsciiColorCodes() throws IOException {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>[^ ]+) (?<msg>.+)", false,
                "yyyy-MM-dd_HH:mm:ssZ", "date",
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
        );

        List<LogRecord> records = loadLog("LogParser/ascii-color-codes.log", logFormat);
        assertEquals("foo", records.get(0).getFieldText("msg"));
        assertEquals("bar\n,fff", records.get(1).getFieldText("msg"));
    }
}
