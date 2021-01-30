package com.logviewer.formats;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogCrashedException;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Record;
import com.logviewer.formats.utils.LvLayoutSimpleDateNode;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

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

        LogFormat logFormat = new LogbackLogFormat(null, pattern);

        Record record = read(logFormat, logRecord);
        checkFields(record, fields);
    }

    @Test
    public void nameCollision() {
        LogFormat logFormat = new LogbackLogFormat(null, "%d %d %d");
        assertEquals(Arrays.asList("date", "date_1", "date_2"), Stream.of(logFormat.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));
    }

    @Test
    public void invalidPattern1() {
        LogFormat logFormat = new LogbackLogFormat(null, "%d %d %");
        TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.getFields());
    }

    @Test
    public void invalidPattern2() {
        LogFormat logFormat = new LogbackLogFormat(null, "%d %d %dfdsfsdf");
        TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.getFields());
    }

    @Test
    public void dateFieldFormat() {
        LogbackLogFormat logFormat = new LogbackLogFormat(null, "[%date{yyyy MM-dd_HH:mm:ss.SSS}] [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n");

        LvLayoutSimpleDateNode dateNode = (LvLayoutSimpleDateNode) Stream.of(logFormat.getDelegate().getLayout())
                .filter(f -> f instanceof LvLayoutSimpleDateNode).findFirst().orElse(null);

        LogFormat.FieldDescriptor dateField = logFormat.getFields()[logFormat.getFieldIndexByName("date")];

        assertEquals("yyyy MM-dd_HH:mm:ss.SSS", dateNode.getFormat());
        assertEquals("date", dateField.name());
        assertEquals(FieldTypes.DATE, dateField.type());
    }



    @Test
    public void ttt() {
        Date date = new Date(101, Calendar.FEBRUARY, 21, 11, 22, 3);

        LoggingEvent event = new LoggingEvent(
                LogbackLogFormatTest.class.getName(),
                (ch.qos.logback.classic.Logger)LOG,
                Level.INFO, "Authentication failed {}", null, new Object[]{100});
        event.setTimeStamp(date.getTime());
        event.setThreadName("localhost-startStop-1-EventThread");

        checkPattern("%-5level [%thread]: %message%n", event,
                "INFO", "localhost-startStop-1-EventThread", "Authentication failed 100");

        checkPattern("%-5level [%thread]: %message%nopexception%nopex%n", event,
                "INFO", "localhost-startStop-1-EventThread", "Authentication failed 100");

        // Date
        checkPattern("%date{q}%n",event, "2001-02-21 11:22:03,000"); // Invalid pattern
        checkPattern("%date{yyyy-MM}%n",event, "2001-02");
        checkPattern("%date%n",event, "2001-02-21 11:22:03,000");
        checkPattern("%F%n",event, "NativeMethodAccessorImpl.java");

        checkPattern("[%date{yyyy-MM-dd_HH:mm:ss.SSS}] [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n", event,
                "2001-02-21_11:22:03.000", "localhost-startStop-1-EventThread", "INFO", "c.l.formats.LogbackLogFormatTest",
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
    }

    @Test
    public void relativeTimestamp() {
        LogFormat format = new LogbackLogFormat("%logger %relative %msg%n");

        Record record = read(format, "com.google.App  10 rrr");

        assertEquals("com.google.App", fieldValue(format, record, "logger"));
        assertEquals("rrr", fieldValue(format, record, "msg"));
        assertEquals("10", fieldValue(format, record, "relativeTime"));
    }

    @Test
    public void relativeTimestampSearch() {
        LogFormat format = new LogbackLogFormat("%thread%relative %msg%n");

        Record record = read(format, "my-thread 10 rrr");

        assertEquals("my-thread", fieldValue(format, record, "thread"));
        assertEquals("rrr", fieldValue(format, record, "msg"));
        assertEquals("10", fieldValue(format, record, "relativeTime"));
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
    public void testReadLog() throws IOException, LogCrashedException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

        Record[] recs = loadLog("default-parser/log.log", FORMAT).toArray(new Record[0]);

        assertEquals("2016-12-02_16:40:47.990", dateFormat.format(new Date(recs[0].getTime())));
        assertEquals("DEBUG", fieldValue(FORMAT, recs[0], "level"));
        assertEquals("http-bio-8088-exec-1", fieldValue(FORMAT, recs[0], "thread"));

        assertEquals("2016-12-02_16:45:26.321", dateFormat.format(new Date(recs[1].getTime())));
        assertEquals("o.a.commons.dbcp2.BasicDataSource", fieldValue(FORMAT, recs[1], "logger"));

        assertEquals("2016-12-02_16:51:35.342", dateFormat.format(new Date(recs[2].getTime())));
        assertEquals("localhost-startStop-1", fieldValue(FORMAT, recs[2], "thread"));
        assertEquals("com.behavox.core.PluginManager", fieldValue(FORMAT, recs[2], "logger"));
        assertEquals("Plugins search time: 197 ms\n", fieldValue(FORMAT, recs[2], "msg"));
    }

    @Test
    public void testNoDate() {
        LogFormat logFormat = new LogbackLogFormat("%d{HH:mm:ss} %msg%n");
        assertFalse(logFormat.hasFullDate());

        Record record = read(logFormat, "10:40:11 aaa");

        assertTrue(record.getTime() <= 0);
    }

}
