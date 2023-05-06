package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.*;
import com.logviewer.utils.LvGsonUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegexLogFormatTest extends AbstractLogTest {

    @Test
    public void testNamedGroupFields() {
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>\\d+) (?<msg>.+)",
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
                );

        LogRecord record = read(logFormat, "111 message text");

        assertEquals("111", record.getFieldText("date"));
        assertEquals("message text", record.getFieldText("msg"));
    }

    @Test
    public void testDateUTC() throws ParseException {
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>[^ ]+) (?<msg>.+)",
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
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>[^ ]+) (?<msg>.+)",
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
        LogFormat logFormat = new RegexLogFormat(
                "(?<level>\\w+) (?<date>\\d[^ ]+)? (?<msg>.+)",
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
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>\\d+) (?<msg>.+)",
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
        ).setDontAppendUnmatchedTextToLastField(true);

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
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>\\d+) (?<msg>.+)",
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
        LogFormat logFormat = new RegexLogFormat(
                "(?<date>[^ ]+) (?<msg>.+)",
                "yyyy-MM-dd_HH:mm:ssZ", "date",
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
        );

        List<LogRecord> records = loadLog("LogParser/ascii-color-codes.log", logFormat);
        assertEquals("foo", records.get(0).getFieldText("msg"));
        assertEquals("bar\n,fff", records.get(1).getFieldText("msg"));
    }

    @Test
    public void testLocale() {
        Locale locale = new Locale("ru", "RU");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd_HH:mm:ssZ", locale).withZone(ZoneId.of("UTC"));

        LogFormat logFormat = new RegexLogFormat(
                "(?<date>[^ ]+) (?<msg>.+)",
                "yyyy-MMM-dd_HH:mm:ssZ", "date",
                RegexLogFormat.field("date", FieldTypes.DATE),
                RegexLogFormat.field("msg", null)
        ).setLocale(locale);

        Instant ts = Instant.parse("2017-04-03T10:15:30.00Z");

        LogRecord read = read(logFormat, formatter.format(ts) + " foo");
        assertEquals(formatter.format(ts), read.getFieldText("date"));

        assertTrue(read.hasTime());
        assertEquals(ts.toEpochMilli(), read.getTimeMillis());
    }

    @Test
    public void serialization() {
//        LogFormat logFormatSample = new RegexLogFormat(
//                "(?<date>[^ ]+) (?<msg>.+)",
//                "yyyy-MM-dd_HH:mm:ssZ", "date",
//                RegexLogFormat.field("date", FieldTypes.DATE),
//                RegexLogFormat.field("msg", null))
//                .withLocale(new Locale("ru", "RU"))
//                .withCharset(StandardCharsets.ISO_8859_1)
//                .withDontAppendUnmatchedTextToLastField(true);
//
//        String json = LvGsonUtils.GSON.toJson(logFormatSample, LogFormat.class);
//        System.out.println(json);
//        if (true)
//            return;

        String serializedRegexpFormat = "{\n" +
                "  \"type\": \"RegexLogFormat\",\n" +
                "  \"charset\": \"ISO-8859-1\",\n" +
                "  \"locale\": \"ru_RU\",\n" +
                "  \"regex\": \"(?\\u003cdate\\u003e[^ ]+) (?\\u003cmsg\\u003e.+)\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"date\",\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"msg\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"dontAppendUnmatchedTextToLastField\": true,\n" +
                "  \"dateFieldName\": \"date\",\n" +
                "  \"datePattern\": \"yyyy-MM-dd_HH:mm:ssZ\"\n" +
                "}";

        RegexLogFormat logFormat = (RegexLogFormat) LvGsonUtils.GSON.fromJson(serializedRegexpFormat, LogFormat.class);

        assertEquals(StandardCharsets.ISO_8859_1, logFormat.getCharset());
        assertEquals(true, logFormat.isDontAppendUnmatchedTextToLastField());
        assertEquals(new Locale("ru", "RU"), logFormat.getLocale());
        assertEquals("yyyy-MM-dd_HH:mm:ssZ", logFormat.getDatePattern());
        assertEquals(Arrays.asList("date", "msg"), Stream.of(logFormat.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));

        String json2 = LvGsonUtils.GSON.toJson(logFormat, LogFormat.class);
        LogFormat logFormat2 = LvGsonUtils.GSON.fromJson(json2, LogFormat.class);


        // Test no transient fields.
        LogRecord record = read(logFormat2, "2011-01-01_23:02:01+0000 foo");
        assertEquals("foo", record.getFieldText("msg"));

        assertEquals(json2, LvGsonUtils.GSON.toJson(logFormat2, LogFormat.class));
        assertEquals(json2, LvGsonUtils.GSON.toJson(LvGsonUtils.copy(logFormat2), LogFormat.class));
    }
}
