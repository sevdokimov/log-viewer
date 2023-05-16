package com.logviewer.formats;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.google.common.collect.ImmutableMap;
import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import com.logviewer.formats.utils.LvLayoutSimpleDateNode;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.utils.LvDateUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@SuppressWarnings("Convert2MethodRef")
public class LogbackLogFormatTest extends AbstractLogTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLogFormatTest.class);

    public static final LogbackLogFormat FORMAT = new LogbackLogFormat("%date{yyyy-MM-dd_HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n");;

    private void checkPattern(String pattern, ILoggingEvent event, String ... fields) {
        PatternLayout layout = new PatternLayout();
        layout.setPattern(pattern);
        layout.setContext(((ch.qos.logback.classic.Logger)LOG).getLoggerContext());
        layout.start();

        String logRecord = layout.doLayout(event);
        if (logRecord.endsWith("\n"))
            logRecord = logRecord.substring(0, logRecord.length() - 1);

        layout.stop();

        LogFormat logFormat = new LogbackLogFormat(pattern);

        LogRecord record = read(logFormat, logRecord);
        checkFields(record, fields);
    }

    @Test
    public void nameCollision() {
        LogFormat logFormat = new LogbackLogFormat("%d %d %d");
        assertEquals(Arrays.asList("date", "date_1", "date_2"), Stream.of(logFormat.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));
    }

    @Test
    public void invalidPattern1() {
        LogFormat logFormat = new LogbackLogFormat("%d %d %");
        TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.getFields());
    }

    @Test
    public void invalidPattern2() {
        LogFormat logFormat = new LogbackLogFormat("%d %d %dfdsfsdf");
        TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.getFields());
    }

    @Test
    public void dateFieldFormat() {
        LogbackLogFormat logFormat = new LogbackLogFormat("[%date{yyyy MM-dd_HH:mm:ss.SSS}] [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n");

        LvLayoutSimpleDateNode dateNode = (LvLayoutSimpleDateNode) Stream.of(logFormat.getDelegate().getLayout())
                .filter(f -> f instanceof LvLayoutSimpleDateNode).findFirst().orElse(null);

        LogFormat.FieldDescriptor dateField = logFormat.getFields()[logFormat.getFieldIndexByName("date")];

        assertEquals("yyyy MM-dd_HH:mm:ss.SSS", dateNode.getFormat());
        assertEquals("date", dateField.name());
        assertEquals(FieldTypes.DATE, dateField.type());
    }

    @Test
    public void testParseRecordWithTimezone() throws Exception {
        LogbackLogFormat format = new LogbackLogFormat("%date{yyyy-MM-dd HH:mm:ss Z} %m%n");

        LogRecord record = read(format, "2011-10-13 18:33:45 +0000 mmm");
        checkFields(record, "2011-10-13 18:33:45 +0000", "mmm");

        Date expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse("2011-10-13 18:33:45 +0000");

        assertEquals(LvDateUtils.toNanos(expectedDate), record.getTime());
    }

    @Test
    public void ttt() {
        Date date = new Date(101, Calendar.FEBRUARY, 21, 11, 22, 3);

        LoggingEvent event = new LoggingEvent(
                LogbackLogFormatTest.class.getName(),
                (ch.qos.logback.classic.Logger)LOG,
                Level.INFO, "Authentication failed {}", null, new Object[]{100});
        event.setTimeStamp(date.getTime());
        event.setMDCPropertyMap(ImmutableMap.of("aaa", "111", "bbb", "222", "ccc", "333"));
        event.setThreadName("localhost-startStop-1-EventThread");

        checkPattern("%-5level [%thread]: %message%n", event,
                "INFO", "localhost-startStop-1-EventThread", "Authentication failed 100");

        checkPattern("%-5level [%X] %message%n", event,
                "INFO", "aaa=111, bbb=222, ccc=333", "Authentication failed 100");
        checkPattern("%-5level %X{aaa} %message%n", event,
                "INFO", "", "111 Authentication failed 100"); // invalid!
        checkPattern("%-5level %X{aaa}-%message%n", event,
                "INFO", "111", "Authentication failed 100");
        checkPattern("%-5level %mdc{zzz:-FF} %message%n", event,
                "INFO", "", "FF Authentication failed 100"); // invalid!
        checkPattern("%-5level [%mdc{zzz:-FF}] %message%n", event,
                "INFO", "FF", "Authentication failed 100");
        checkPattern("%-5level [%thread]: %message%nopexception%nopex%n", event,
                "INFO", "localhost-startStop-1-EventThread", "Authentication failed 100");

        // Date
        checkPattern("%date{q}%n",event, "2001-02-21 11:22:03,000"); // Invalid pattern
        checkPattern("%date{yyyy-MM-dd}%n",event, "2001-02-21");
        checkPattern("%date{yyyy-MMM}%n",event, "2001-Feb");
        checkPattern("%date{yyyyMMddHHmmss}%n",event, "20010221112203");
        checkPattern("%date%n",event, "2001-02-21 11:22:03,000");
        checkPattern("%F%n",event, "NativeMethodAccessorImpl.java");

        checkPattern("[%date{yyyy-MM-dd_HH:mm:ss.SSS}] [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n", event,
                "2001-02-21_11:22:03.000", "localhost-startStop-1-EventThread", "INFO", "c.l.formats.LogbackLogFormatTest",
                "Authentication failed 100");

        checkPattern("%date{yyyy-MM-dd HH:mm:ss.SSSSSS} [%level] from %logger in %thread - %message%n%xException", event,
                "2001-02-21 11:22:03.000000", "INFO", "com.logviewer.formats.LogbackLogFormatTest", "localhost-startStop-1-EventThread",
                "Authentication failed 100");

        checkPattern("%date{yyyy-MM-dd HH:mm:ss ZZZZ} [%level] from %logger in %thread - %message%n%xException", event,
                "2001-02-21 11:22:03 +0300", "INFO", "com.logviewer.formats.LogbackLogFormatTest", "localhost-startStop-1-EventThread",
                "Authentication failed 100");

        checkPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n", event,
                "11:22:03.000", "localhost-startStop-1-EventThread", "INFO", "c.l.formats.LogbackLogFormatTest",
                "Authentication failed 100");

        checkPattern("%date{yyyy-MM-dd HH:mm:ss ZZZZ} [%level] from %logger in %thread - .%M\\(%line\\) - %message%n%xException", event,
                "2001-02-21 11:22:03 +0300", "INFO", "com.logviewer.formats.LogbackLogFormatTest",
                "localhost-startStop-1-EventThread", "invoke0", "-2",
                "Authentication failed 100");

        checkPattern("[%level] %date{yyyy-MM-dd HH:mm:ss.SSS} %logger{96}:[%line] - %msg%n", event,
                "INFO", "2001-02-21 11:22:03.000", "com.logviewer.formats.LogbackLogFormatTest", "-2",
                "Authentication failed 100");

        checkPattern("%d{MM-dd-yyyy:HH:mm:ss.SSS} [%thread] %-5level %logger{10}->%method\\(\\):%line - %msg%n", event,
                "02-21-2001:11:22:03.000", "localhost-startStop-1-EventThread", "INFO",
                "c.l.f.LogbackLogFormatTest", "invoke0", "-2", "Authentication failed 100");

        checkPattern("%d %-5level [%thread] %logger{0}: %msg%n", event,
                "2001-02-21 11:22:03,000", "INFO", "localhost-startStop-1-EventThread",
                "LogbackLogFormatTest", "Authentication failed 100");

        checkPattern("%-5level %d{yy-MM-dd HH:mm:ss}[%thread] [%logger{0}:%line] - %msg%n", event,
                "INFO", "01-02-21 11:22:03", "localhost-startStop-1-EventThread",
                "LogbackLogFormatTest", "-2", "Authentication failed 100");

        checkPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread/%X{UNIQUE_ID}] %logger{36}:%line - %msg%n", event,
                "2001-02-21 11:22:03.000", "INFO", "localhost-startStop-1-EventThread", "", "c.l.formats.LogbackLogFormatTest",
                "-2", "Authentication failed 100");

        checkPattern("%d{yyyy-MM-dd HH:mm:ss, Asia/Seoul} %-10level [%L] [%.-24thread] %logger{50} %ex{30} - %msg%n", event,
                "2001-02-21 17:22:03", "INFO", "-2", "localhost-startStop-1-Ev", "com.logviewer.formats.LogbackLogFormatTest",
                "", "Authentication failed 100");
    }

    @Test
    public void relativeTimestamp() {
        LogFormat format = new LogbackLogFormat("%logger %relative %msg%n");

        LogRecord record = read(format, "com.google.App  10 rrr");

        assertEquals("com.google.App", record.getFieldText("logger"));
        assertEquals("rrr", record.getFieldText("msg"));
        assertEquals("10", record.getFieldText("relativeTime"));
    }

    @Test
    public void relativeTimestampSearch() {
        LogFormat format = new LogbackLogFormat("%thread%relative %msg%n");

        LogRecord record = read(format, "my-thread 10 rrr");

        assertEquals("my-thread", record.getFieldText("thread"));
        assertEquals("rrr", record.getFieldText("msg"));
        assertEquals("10", record.getFieldText("relativeTime"));
    }

    @Test
    public void relativeTimestamp2() {
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();

        LoggingEvent event = new LoggingEvent(
                LogbackLogFormatTest.class.getName(),
                (ch.qos.logback.classic.Logger)LOG,
                Level.ERROR, "Authentication failed {}", null, new Object[]{100});
        event.setTimeStamp(startTime + 3000);
        event.setThreadName("my-thread");

        checkPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n", event,
                "~\\d+", "my-thread", "ERROR", "c.l.formats.LogbackLogFormatTest", "Authentication failed 100");


        event = new LoggingEvent(
                LogbackLogFormatTest.class.getName(),
                (ch.qos.logback.classic.Logger)LOG,
                Level.ERROR, "Authentication failed {}", null, new Object[]{100});
        event.setTimeStamp(startTime + 1000000);
        event.setThreadName("my-thread");

        checkPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n", event,
                "~\\d+", "my-thread", "ERROR", "c.l.formats.LogbackLogFormatTest", "Authentication failed 100");
    }

    @Test
    public void strangeThreadName() {
        Date date = new Date(101, Calendar.FEBRUARY, 21, 11, 22, 3);

        String[] threadNames = {"localhost   startStop  1-EventThread", "-", "] [ - ]", "]", "] INFO"};

        for (String threadName : threadNames) {
            LoggingEvent event = new LoggingEvent(
                    LogbackLogFormatTest.class.getName(),
                    (ch.qos.logback.classic.Logger)LOG,
                    Level.INFO, "Authentication failed {}", null, new Object[]{100});
            event.setTimeStamp(date.getTime());
            event.setThreadName(threadName);

            checkPattern("[%date{yyyy-MM-dd_HH:mm:ss.SSS}] [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n", event,
                    "2001-02-21_11:22:03.000", threadName, "INFO", "c.l.formats.LogbackLogFormatTest",
                    "Authentication failed 100");
        }
    }

    @Test
    public void testReadLog() throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

        LogRecord[] recs = loadLog("default-parser/log.log", FORMAT).toArray(new LogRecord[0]);

        assertEquals("2016-12-02_16:40:47.990", dateFormat.format(new Date(recs[0].getTimeMillis())));
        assertEquals("DEBUG", recs[0].getFieldText("level"));
        assertEquals("http-bio-8088-exec-1", recs[0].getFieldText("thread"));

        assertEquals("2016-12-02_16:45:26.321", dateFormat.format(new Date(recs[1].getTimeMillis())));
        assertEquals("o.a.commons.dbcp2.BasicDataSource", recs[1].getFieldText("logger"));

        assertEquals("2016-12-02_16:51:35.342", dateFormat.format(new Date(recs[2].getTimeMillis())));
        assertEquals("localhost-startStop-1", recs[2].getFieldText("thread"));
        assertEquals("com.behavox.core.PluginManager", recs[2].getFieldText("logger"));
        assertEquals("Plugins search time: 197 ms\n", recs[2].getFieldText("msg"));
    }

    @Test
    public void testNoDate() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %msg%n");
        assertFalse(logFormat.hasFullDate());

        LogRecord record = read(logFormat, "10:40:11 aaa");

        assertTrue(record.getTime() <= 0);
    }

    @Test
    public void testwEx() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %msg%n%wEx");
        LogRecord record = read(logFormat, "10:40:11 aaa");

        assertTrue(record.getTime() <= 0);
    }

    @Test
    public void testProcessId() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %processId %msg%wEx");

        LogRecord record1 = read(logFormat, "10:40:11 1 aaa");
        assertEquals("1", record1.getFieldText("pid"));

        LogRecord record2 = read(logFormat, "10:40:11 1111 aaa");
        assertEquals("1111", record2.getFieldText("pid"));
    }

    @Test
    public void testMdcPropertyName() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %X{aaa} %msg%wEx");
        LogFormat.FieldDescriptor[] fields = logFormat.getFields();

        assertEquals(Arrays.asList("date", "aaa", "msg"), Stream.of(fields).map(f -> f.name()).collect(Collectors.toList()));

        LogFormat.FieldDescriptor field = fields[1];
        assertEquals(FieldTypes.MDC, field.type());
    }

    @Test
    public void testMdcPropertyNameWithDefault() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %X{aaa:-} %msg%wEx");
        LogFormat.FieldDescriptor[] fields = logFormat.getFields();

        assertEquals(Arrays.asList("date", "aaa", "msg"), Stream.of(fields).map(f -> f.name()).collect(Collectors.toList()));

        LogFormat.FieldDescriptor field = fields[1];
        assertEquals(FieldTypes.MDC, field.type());
    }

    @Test
    public void noMdcBeforeMessage() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %X{aaa}%msg%wEx");
        LogFormat.FieldDescriptor[] fields = logFormat.getFields();
        assertEquals(2, fields.length); // Don't create MDC node before message node, the parser cannot parse that.
        assertEquals("msg", fields[1].name());
    }

    @Test
    public void additionalLevelNames() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %level %msg%wEx");
        assertEquals("WARN", read(logFormat, "12:00:00 WARN aaa").getFieldText("level"));
        assertEquals("WARNING", read(logFormat, "12:00:00 WARNING aaa").getFieldText("level"));
    }

    @Test
    public void testLocale() {
        String pattern = "%d{yyyy MMM dd HH:mm:ss Z} %m%n";
        Instant ts = Instant.parse("2007-01-03T10:15:30.00Z");
        Locale customLocale = new Locale("ru", "RU");

        String str = TestUtils.withLocale(customLocale, () -> {
            LoggingEvent event = new LoggingEvent(
                    LogbackLogFormatTest.class.getName(),
                    (ch.qos.logback.classic.Logger) LOG,
                    Level.ERROR, "foo", null, new Object[]{});

            event.setTimeStamp(ts.toEpochMilli());

            PatternLayout layout = new PatternLayout();
            layout.setPattern(pattern);
            layout.setContext(((ch.qos.logback.classic.Logger)LOG).getLoggerContext());
            layout.start();

            return layout.doLayout(event);
        });

        assertTrue(str, str.contains("янв")); // "2011 окт 13" in Java 8, "2011 окт. 13" in Java 17

        LogFormat logFormat = new Log4jLogFormat(pattern).setLocale(customLocale);
        LogRecord read = read(logFormat, str);

        assertTrue(read.hasTime());
        assertTrue(read.getFieldText("date").matches("2007 янв.? 03.*")); // "2007 янв 03" in Java 8, "2007 янв. 03" in Java 17
        assertEquals(ts.toEpochMilli(), read.getTimeMillis());
    }

    @Test
    public void customLevel() {
        LogbackLogFormat format = new LogbackLogFormat("%d{yyyy-MM-dd_HH:mm:ss} [%level] %msg%n");
        assertEquals(Collections.emptyList(), format.getCustomLevels());
        format.addCustomLevels(Collections.singletonList("XXX"));
        assertEquals(Collections.singletonList("XXX"), format.getCustomLevels());

        LogRecord record = read(format, "2017-11-24 11:00:00 [XXX] foo");
        assertEquals("XXX", record.getFieldText("level"));
    }
}
