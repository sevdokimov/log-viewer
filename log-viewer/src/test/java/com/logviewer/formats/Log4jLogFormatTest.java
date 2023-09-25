package com.logviewer.formats;

import com.google.common.collect.ImmutableMap;
import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.*;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.utils.LvDateUtils;
import com.logviewer.utils.LvGsonUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class Log4jLogFormatTest extends AbstractLogTest {

    @Test
    public void unsupportedPattern() {
        Log4jLogFormat format = new Log4jLogFormat("%-5p %n [%t]: %m%n");

        TestUtils.assertError(IllegalArgumentException.class, format::createReader);
    }

    @Test
    public void testNDC() {
        check("%p %NDC %m%n", createEvent(), true, "ERROR", "", "The log message");

        read(new Log4jLogFormat("%p %NDC %m%n"), "ERROR [] dffsdsf");
        notMatch("%p %NDC %m%n", "ERROR  dffsdsf");
    }

    @Test
    public void testHbaseLog() {
        String s = "2020-06-13 00:02:48,475 WARN  [HBase-Metrics2-1] impl.MetricsConfig: Cannot locate configuration: tried hadoop-metrics2-phoenix.properties,hadoop-metrics2.properties";
        LogRecord read = read(new Log4jLogFormat("%d{ISO8601} %-5p [%t] %c{2}: %m%n"), s);
        checkFields(read, "2020-06-13 00:02:48,475", "WARN", "HBase-Metrics2-1", "impl.MetricsConfig",
                "Cannot locate configuration: tried hadoop-metrics2-phoenix.properties,hadoop-metrics2.properties");
    }

    @Test
    public void testMDC() {
        check("%p %NDC %m%n", createEvent(), true, "ERROR", "", "The log message");

        read(new Log4jLogFormat("%p %X %m%n"), "ERROR {} dffsdsf");
        notMatch("%p %X %m%n", "ERROR xxx dffsdsf");

//        read(new Log4jLogFormat("%p %X{aaa} %m%n"), "ERROR  dffsdsf");  todo bug!
        LogRecord record = read(new Log4jLogFormat("%p %X{aaa} %m%n"), "ERROR xxx dffsdsf");
        checkFields(record, "ERROR", "xxx", "dffsdsf");
    }

    @Test
    public void testParseRecordWithTimezone() throws Exception {
        Log4jLogFormat format = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss Z} %m%n");

        LogRecord record = read(format, "2011-10-13 18:33:45 +0000 mmm");
        checkFields(record, "2011-10-13 18:33:45 +0000", "mmm");

        Date expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse("2011-10-13 18:33:45 +0000");

        assertEquals(LvDateUtils.toNanos(expectedDate), record.getTime());
    }

    @Test
    public void testLogLevelFine() {
        Log4jLogFormat format = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss,SSS} %level %m%n");

        LogRecord record = read(format, "2022-09-01 13:08:09,493 FINE [javax.faces.component] (default task-1) No renderer-type for component j_idt3");
        checkFields(record, "2022-09-01 13:08:09,493", "FINE", "[javax.faces.component] (default task-1) No renderer-type for component j_idt3");
    }

    @Test
    public void testLog4j() throws Exception {
        Throwable throwable = new Throwable();

        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(new Date(111, Calendar.OCTOBER, 13, 18, 33, 45).getTime(), 777);

        Supplier<Log4jLogEvent.Builder> builderFactory = () -> Log4jLogEvent.newBuilder()
                .setInstant(instant)
                .setLevel(Level.ERROR)
                .setMessage(new ObjectMessage("The log message"))
                .setSource(throwable.getStackTrace()[0])
                .setThreadId(12123)
                .setThreadName("thread-pool-11")
                .setLoggerFqcn("com.google.gson.Gson")
                .setLoggerName("com.google.gson.Gson")
                .setContextStack(new MutableThreadContextStack(Arrays.asList("aaa", "bbb")))
                .setMarker(MarkerManager.getMarker("m-a-r-k-e-r"));

        LogEvent event = builderFactory.get().build();

        // "2011-11-13 18:33:45"
        check("%levelzzz{www}%n", event, true, "ERROR");
        check("%levelzzz%n", event, true, "ERROR");
        check("%levelzzz{}%n", event, true, "ERROR");
        check("%%l%level%n", event, true, "ERROR");

        check1("%-5p [%t]: %m%n", event, true, "ERROR", "thread-pool-11", "The log message");
        check1("[%d{ISO8601}][%-5p][%t][%c{1}] %m%n", event, "~2011-10-13.18:33:45,000", "ERROR", "thread-pool-11", "Gson", "The log message");
        check1("%d{ABSOLUTE} %m%n", event, true, "18:33:45,000", "The log message");

        check1("%d{yyyyMMddHHmmss} %m%n", event, "20111013183345", "The log message");
        check1("%d{yyyyMMddHHmmssSSS} %m%n", event, "20111013183345000", "The log message");

        check1("%d{yyyyMMddHHmmss.SSS} %m%n", event, "20111013183345.000", "The log message");
        check1("%d{yyyyMMddHHmmss.SSSS} %m%n", event, "20111013183345.0000", "The log message");
        check1("%d{yyyyMMddHHmmss.SSSSS} %m%n", event, "20111013183345.00000", "The log message");
        check1("%d{yyyyMMddHHmmss.SSSSSS} %m%n", event, "20111013183345.000000", "The log message");
        check1("%d{yyyyMMddHHmmss.SSSSSSSSS} %m%n", event, "20111013183345.000000000", "The log message");

        TestUtils.withTimeZone("UTC", () -> {
            check1("%d{yyyyMMddHHmmssSSS z} %m%n", event, "20111013143345000 UTC", "The log message");
            check1("%d{yyyyMMddHHmmssSSS zz} %m%n", event, "20111013143345000 UTC", "The log message");
            check1("%d{yyyyMMddHHmmssSSS zzz} %m%n", event, "20111013143345000 UTC", "The log message");
            check1("%d{yyyyMMddHHmmssSSS zzzz} %m%n", event, "20111013143345000 Coordinated Universal Time", "The log message");

            check1("%d{yyyyMMddHHmmssSSS Z} %m%n", event, "20111013143345000 +0000", "The log message");

//            check1("%d{yyyyMMddHHmmssSSS ZZZ} %m%n", event, "20111013143345000 +00:00", "The log message"); todo
//            check1("%d{yyyyMMddHHmmssSSS ZZZZ} %m%n", event, "20111013143345000 +00:00", "The log message");
//            check1("%d{yyyyMMddHHmmssSSS ZZZZZ} %m%n", event, "20111013143345000 +00:00", "The log message");
        });

        check("%d{yyyyMMdd HHmmss}{EST} %m%n", event, "20111013 093345", "The log message");
        check("%d{yyyyMMdd HHmmss}{UTC} %m%n", event, "20111013 143345", "The log message");
        check1("%d{DATE} %m%n", event, "13 Oct 2011 18:33:45,000", "The log message");
        check1("%d{HH:mm:ss,SSS} %5p [%t] - %m%n", event, true, "18:33:45,000", "ERROR", "thread-pool-11", "The log message");
        check1("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n", event, "2011-10-13 18:33:45", "ERROR", "Gson", "~\\d+", "The log message");

        check1("[%d{dd-MM-yyyy HH:mm:ss}][%-5p][%t][%c{1}] %m%n", event, "13-10-2011 18:33:45", "ERROR", "thread-pool-11", "Gson", "The log message");
        check1("[%d{dd-MMM-yyyy HH:mm:ss}][%-5p][%t][%c{1}] %m%n", event, "13-Oct-2011 18:33:45", "ERROR", "thread-pool-11", "Gson", "The log message");

        check1("[%p] %m%n", event, true, "ERROR", "The log message");
        check1("[%p] %d %m%n", event, true, "ERROR", "2011-10-13 18:33:45,000", "The log message");
        check1("[%30.30t] %-30.30c{1} %-5p %m%n", event, true, "thread-pool-11", "Gson", "ERROR", "The log message");
      //  check("%d [%-15.15t] %-5p %-30.30c{1} - %m%n", event, "2011-10-13 18:33:45,000", "thread-pool-11", "Gson", "ERROR", "The log message");  todo bug!
        check1("%d{ISO8601} [%-5p][%t][%30c] - [%X] %m%n", event, "~2011-10-13.18:33:45,000", "ERROR", "thread-pool-11",
                "com.google.gson.Gson", "", "The log message");

        check("%d{yyyy-MM-dd} %logger %class %file %location %method (%marker) (%markerSimpleName) %nano %pid " +
                        "%sequenceNumber %threadId %thread %threadPriority %fqcn %endOfBatch " +
                        "{%NDC} {%MDC} " +
                        "%uuid %% %exception%m%n",
                event, true,
                "2011-10-13", "com.google.gson.Gson", "com.logviewer.formats.Log4jLogFormatTest",
                "Log4jLogFormatTest.java",
                "~com\\.logviewer\\.formats\\.Log4jLogFormatTest\\.testLog4j\\(Log4jLogFormatTest\\.java:\\d+\\)",
                "testLog4j", "m-a-r-k-e-r", "m-a-r-k-e-r", "0", getProcessId(), "1", "12123",
                "thread-pool-11", "5", "com.google.gson.Gson", "false",
                "aaa, bbb", "",
                "~[a-f\\-0-9]+",
                "The log message"
        );

        check1("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p {%X{username} %X{client} %X{location}} %t [%c]: %m%n", event,
                "2011-10-13 18:33:45,000", "ERROR", "", "", "", "thread-pool-11", "com.google.gson.Gson", "The log message");

        check("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p {%X{username} %X{client} %X{location}} %t [%c]: %m%n",
                builderFactory.get().setLevel(Level.INFO)
                        .setContextData(new SortedArrayStringMap(ImmutableMap.of("username", "smith", "location", "London")))
                        .build(),
                "2011-10-13 18:33:45,000", "INFO", "smith", "", "London", "thread-pool-11", "com.google.gson.Gson", "The log message");

        check("%date{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %5level %logger{36} - %msg%n", event, "13-10-2011 18:33:45.000", "thread-pool-11", "ERROR", "com.google.gson.Gson", "The log message");
        check("%d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %5level %logger{36} - %msg%n", event, "13-10-2011 18:33:45.000", "thread-pool-11", "ERROR", "com.google.gson.Gson", "The log message");
    }

    private String getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.substring(0, name.indexOf("@"));
    }

    private void check(String pattern, LogEvent event, String ... fields) {
        check(pattern, event, false, fields);
    }

    private void check(String pattern, LogEvent event, boolean noTimestamp, String ... fields) {
        PatternLayout layout = PatternLayout.newBuilder().withPattern(pattern).build();

        String str = layout.getEventSerializer().toSerializable(event);

        checkParsing(event, str, pattern, noTimestamp, fields);
    }

    private void check1(String pattern, LogEvent event, String ... fields) {
        check1(pattern, event, false, fields);
    }

    private void check1(String pattern, LogEvent event, boolean noTimestamp, String ... fields) {
        check(pattern, event, noTimestamp, fields);

        org.apache.log4j.PatternLayout patternLayout1 = new org.apache.log4j.PatternLayout(pattern);

        String str = patternLayout1.format(new LoggingEvent(event.getLoggerFqcn(),
                Logger.getLogger(event.getLoggerName()),
                event.getTimeMillis(),
                org.apache.log4j.Level.toLevel(event.getLevel().name()), event.getMessage(), event.getThreadName(), null,
                "ndc", new LocationInfo("Log4jLogFormatTest.java", "Log4jLogFormatTest", "testLog4j", "35"), null));

        checkParsing(event, str, pattern, noTimestamp, fields);
    }

    private void checkParsing(LogEvent event, String str, String pattern, boolean noTimestamp, String[] fields) {
        if (str.endsWith("\n"))
            str = str.substring(0, str.length() - 1);

        Log4jLogFormat format = new Log4jLogFormat(pattern);

        LogRecord record = read(format, str);
        checkFields(record, fields);

        if (!noTimestamp)
            assertTrue(record.hasTime());

        if (record.hasTime())
            assertEquals(event.getTimeMillis(), TimeUnit.NANOSECONDS.toMillis(record.getTime()));
    }

    private void notMatch(String pattern, String line) {
        Log4jLogFormat format = new Log4jLogFormat(pattern);

        LogReader reader = format.createReader();
        boolean isSuccess = reader.parseRecord(new BufferedFile.Line(line));
        assertFalse(isSuccess);
    }

    @Test
    public void additionalLevelNames() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %level %msg%wEx");
        assertEquals("WARN", read(logFormat, "12:00:00 WARN aaa").getFieldText("level"));
        assertEquals("WARNING", read(logFormat, "12:00:00 WARNING aaa").getFieldText("level"));
    }

    @Test
    public void testAsciiColorCodes() throws IOException {
        LogFormat logFormat = new Log4jLogFormat("%d{yyyy-MM-dd_HH:mm:ss} %m%n");

        List<LogRecord> records = loadLog("LogParser/ascii-color-codes.log", logFormat);
        assertEquals("foo", records.get(0).getFieldText("msg"));
        assertEquals("bar\n,fff", records.get(1).getFieldText("msg"));
    }

    private static Log4jLogEvent createEvent() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(new Date(111, Calendar.OCTOBER, 13, 18, 33, 45).getTime(), 777);

        return Log4jLogEvent.newBuilder()
                .setInstant(instant)
                .setLevel(Level.ERROR)
                .setMessage(new ObjectMessage("The log message"))
                .setContextStack(ThreadContext.EMPTY_STACK)
                .build();
    }

    @Test
    public void testLocale() {
        String pattern = "%d{yyyy MMM dd HH:mm:ss} %m%n";
        Locale customLocale = new Locale("ru", "RU");

        String str = TestUtils.withLocale(customLocale, () -> {
            PatternLayout layout = PatternLayout.newBuilder().withPattern(pattern).build();

            return layout.getEventSerializer().toSerializable(createEvent());
        });

        assertTrue(str.contains("окт")); // "2011 окт 13" in Java 8, "2011 окт. 13" in Java 17

        LogFormat logFormat = new Log4jLogFormat(pattern).setLocale(customLocale);
        LogRecord read = read(logFormat, str);

        assertTrue(read.hasTime());
        assertTrue(read.getFieldText("date").matches("2011 окт.? 13 18:33:45")); // "2011 окт 13" in Java 8, "2011 окт. 13" in Java 17
    }

    @Test
    public void serialization() {
//        LogFormat logFormatSample = new Log4jLogFormat("%d{HH:mm:ss} %processId %msg%wEx")
//                .setRealLog4j(false)
//                .setLocale(new Locale("ru", "RU"))
//                .setCharset(StandardCharsets.ISO_8859_1);
//
//        String json = LvGsonUtils.GSON.toJson(logFormatSample, LogFormat.class);
//        System.out.println(json);
//        if (true)
//            return;

        String serializedFormat = "{\n" +
                "  \"type\": \"Log4jLogFormat\",\n" +
                "  \"realLog4j\": false,\n" +
                "  \"charset\": \"ISO-8859-1\",\n" +
                "  \"locale\": \"ru_RU\",\n" +
                "  \"customLevels\": [\"XXX\", \"YYY\"],\n" +
                "  \"pattern\": \"%d{yyyy-MM-dd HH:mm:ss Z} %m%n\"\n" +
                "}\n";

        Log4jLogFormat logFormat = (Log4jLogFormat) LvGsonUtils.GSON.fromJson(serializedFormat, LogFormat.class);

        assertEquals(StandardCharsets.ISO_8859_1, logFormat.getCharset());
        assertEquals(false, logFormat.isRealLog4j());
        assertEquals(new Locale("ru", "RU"), logFormat.getLocale());
        assertEquals("%d{yyyy-MM-dd HH:mm:ss Z} %m%n", logFormat.getPattern());
        assertEquals(Arrays.asList("XXX", "YYY"), logFormat.getCustomLevels());

        String json2 = LvGsonUtils.GSON.toJson(logFormat, LogFormat.class);

        LogFormat logFormat2 = LvGsonUtils.GSON.fromJson(json2, LogFormat.class);


        // Test no transient fields.
        LogRecord record = read(logFormat2, "2011-01-01 23:02:01 +0000 foo");
        assertEquals("foo", record.getFieldText("msg"));

        assertEquals(json2, LvGsonUtils.GSON.toJson(logFormat2, LogFormat.class));
        assertEquals(json2, LvGsonUtils.GSON.toJson(LvGsonUtils.copy(logFormat2), LogFormat.class));
    }

    @Test
    public void customLevel() {
        Log4jLogFormat format = new Log4jLogFormat("%d{yyyy-MM-dd_HH:mm:ss} [%level] %msg%n");
        format.addCustomLevels(Collections.singletonList("XXX"));

        LogRecord record = read(format, "2017-11-24_11:00:00 [XXX] foo");
        assertEquals("XXX", record.getFieldText("level"));
    }

    @Test
    public void customLevelFromGlobalConfig() {
        TestUtils.withSystemProp("log-viewer.log-levels", "XXX,YYY", () -> {
            Log log = getLogService().openLog(getTestLog("format-detection/level-date.log"));
            AbstractPatternLogFormat format = (AbstractPatternLogFormat) log.getFormat();
            assertEquals(Arrays.asList("XXX", "YYY"), format.getCustomLevels());
        });

        Log log = getLogService().openLog(getTestLog("format-detection/level-date.log"));
        AbstractPatternLogFormat format = (AbstractPatternLogFormat) log.getFormat();
        assertEquals(Collections.emptyList(), format.getCustomLevels());
    }

    @Test
    public void multiPattern() throws IOException {
        Log4jLogFormat format = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss} %p %m%n", "[%p] %d{yyyy-MM-dd HH:mm:ss} %m%n");

        assertEquals(Arrays.asList("date", "level", "msg"), Stream.of(format.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));

        String file = getTestLog("multi-pattern.log");

        List<LogRecord> log = new ArrayList<>();
        readLog(log, getLogService().openLog(file, format), false);

        assertEquals("foo", log.get(0).getMessage());

        checkFields(log.get(1), "2021-08-07 11:00:00", "INFO", "message in format \"%d %p %m%n\"");
        checkFields(log.get(2), "2021-08-07 12:00:00", "WARN", "message in format \"[%p] %d %m%n\"");
        checkFields(log.get(3), "2021-08-07 13:00:00", "WARN", "message in format \"[%p] %d %m%n\"\nfoo\nbar");
        checkFields(log.get(4), "2021-08-07 14:00:00", "WARN", "message in format \"[%p] %d %m%n\"");
        checkFields(log.get(5), "2021-08-07 15:00:00", "WARN", "message in format \"[%p] %d %m%n\"");
        checkFields(log.get(6), "2021-08-07 16:00:00", "ERROR", "message in format \"%d %p %m%n\"\nxxx\nyyy");

        List<LogRecord> logBackwardRead = new ArrayList<>();
        readLog(logBackwardRead, getLogService().openLog(file, format), true);
        Collections.reverse(logBackwardRead);

        assertArrayEquals(log.stream().map(LogRecord::getMessage).toArray(), logBackwardRead.stream().map(LogRecord::getMessage).toArray());
    }

    private void readLog(List<LogRecord> res, Log log, boolean backward) throws IOException {
        try (Snapshot snapshot = log.createSnapshot()) {
            if (backward) {
                snapshot.processRecordsBack(snapshot.getSize(), true, record -> {
                    res.add(record);
                    return true;
                });
            } else {
                snapshot.processRecords(0, record -> {
                    res.add(record);
                    return true;
                });
            }
        }
    }
}
