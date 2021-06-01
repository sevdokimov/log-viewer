package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.BufferedFile;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.LogRecord;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
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
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertFalse;

public class Log4jLogFormatTest extends AbstractLogTest {

    @Test
    public void unsupportedPattern() {
        Log4jLogFormat format = new Log4jLogFormat("%-5p %n [%t]: %m%n");

        TestUtils.assertError(IllegalArgumentException.class, format::createReader);
    }

    @Test
    public void testNDC() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(new Date(111, Calendar.OCTOBER, 13, 18, 33, 45).getTime(), 777);

        LogEvent event = Log4jLogEvent.newBuilder()
                .setInstant(instant)
                .setLevel(Level.ERROR)
                .setMessage(new ObjectMessage("The log message"))
                .setContextStack(ThreadContext.EMPTY_STACK)
                .build();

        check("%p %NDC %m%n", event, "ERROR", "", "The log message");

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
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(new Date(111, Calendar.OCTOBER, 13, 18, 33, 45).getTime(), 777);

        LogEvent event = Log4jLogEvent.newBuilder()
                .setInstant(instant)
                .setLevel(Level.ERROR)
                .setMessage(new ObjectMessage("The log message"))
                .setContextStack(ThreadContext.EMPTY_STACK)
                .build();

        check("%p %NDC %m%n", event, "ERROR", "", "The log message");

        read(new Log4jLogFormat("%p %X %m%n"), "ERROR {} dffsdsf");
        notMatch("%p %X %m%n", "ERROR xxx dffsdsf");

//        read(new Log4jLogFormat("%p %X{aaa} %m%n"), "ERROR  dffsdsf");  todo bug!
        LogRecord record = read(new Log4jLogFormat("%p %X{aaa} %m%n"), "ERROR xxx dffsdsf");
        checkFields(record, "ERROR", "xxx", "dffsdsf");
    }

    @Test
    public void testLog4j() {
        Throwable throwable = new Throwable();

        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(new Date(111, Calendar.OCTOBER, 13, 18, 33, 45).getTime(), 777);

        LogEvent event = Log4jLogEvent.newBuilder()
                .setInstant(instant)
                .setLevel(Level.ERROR)
                .setMessage(new ObjectMessage("The log message"))
                .setSource(throwable.getStackTrace()[0])
                .setThreadId(12123)
                .setThreadName("thread-pool-11")
                .setLoggerFqcn("com.google.gson.Gson")
                .setLoggerName("com.google.gson.Gson")
                .setContextStack(new MutableThreadContextStack(Arrays.asList("aaa", "bbb")))
                .setMarker(MarkerManager.getMarker("m-a-r-k-e-r"))
                .build();

        // "2011-11-13 18:33:45"
        check("%levelzzz{www}%n", event, "ERROR");
        check("%levelzzz%n", event, "ERROR");
        check("%levelzzz{}%n", event, "ERROR");
        check("%%l%level%n", event, "ERROR");

        check1("%-5p [%t]: %m%n", event, "ERROR", "thread-pool-11", "The log message");
        check1("[%d{ISO8601}][%-5p][%t][%c{1}] %m%n", event, "~2011-10-13.18:33:45,000", "ERROR", "thread-pool-11", "Gson", "The log message");
        check1("%d{ABSOLUTE} %m%n", event, "18:33:45,000", "The log message");

        check("%d{yyyyMMdd HHmm}{EST} %m%n", event, "20111013 0933", "The log message");
        check("%d{yyyyMMdd HHmm}{UTC} %m%n", event, "20111013 1433", "The log message");
        check1("%d{DATE} %m%n", event, "13 Oct 2011 18:33:45,000", "The log message");
        check1("%d{HH:mm:ss,SSS} %5p [%t] - %m%n", event, "18:33:45,000", "ERROR", "thread-pool-11", "The log message");
        check1("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n", event, "2011-10-13 18:33:45", "ERROR", "Gson", "~\\d+", "The log message");

        check1("[%d{dd-MM-yyyy HH:mm:ss}][%-5p][%t][%c{1}] %m%n", event, "13-10-2011 18:33:45", "ERROR", "thread-pool-11", "Gson", "The log message");
        check1("[%d{dd-MMM-yyyy HH:mm:ss}][%-5p][%t][%c{1}] %m%n", event, "13-Oct-2011 18:33:45", "ERROR", "thread-pool-11", "Gson", "The log message");

        check1("[%p] %m%n", event, "ERROR", "The log message");
        check1("[%p] %d %m%n", event, "ERROR", "2011-10-13 18:33:45,000", "The log message");
        check1("[%30.30t] %-30.30c{1} %-5p %m%n", event, "thread-pool-11", "Gson", "ERROR", "The log message");
      //  check("%d [%-15.15t] %-5p %-30.30c{1} - %m%n", event, "2011-10-13 18:33:45,000", "thread-pool-11", "Gson", "ERROR", "The log message");  todo bug!
        check1("%d{ISO8601} [%-5p][%t][%30c] - [%X] %m%n", event, "~2011-10-13.18:33:45,000", "ERROR", "thread-pool-11",
                "com.google.gson.Gson", "", "The log message");

        check("%d{yyyy-MM-dd} %logger %class %file %location %method (%marker) (%markerSimpleName) %nano %pid " +
                        "%sequenceNumber %threadId %thread %threadPriority %fqcn %endOfBatch " +
                        "{%NDC} {%MDC} " +
                        "%uuid %% %exception%m%n",
                event,
                "2011-10-13", "com.google.gson.Gson", "com.logviewer.formats.Log4jLogFormatTest",
                "Log4jLogFormatTest.java",
                "~com\\.logviewer\\.formats\\.Log4jLogFormatTest\\.testLog4j\\(Log4jLogFormatTest\\.java:\\d+\\)",
                "testLog4j", "m-a-r-k-e-r", "m-a-r-k-e-r", "0", getProcessId(), "1", "12123",
                "thread-pool-11", "5", "com.google.gson.Gson", "false",
                "aaa, bbb", "",
                "~[a-f\\-0-9]+",
                "The log message"
        );
    }


    private String getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.substring(0, name.indexOf("@"));
    }

    private void check(String pattern, LogEvent event, String ... fields) {
        PatternLayout layout = PatternLayout.newBuilder().withPattern(pattern).build();

        String str = layout.getEventSerializer().toSerializable(event);

        if (str.endsWith("\n"))
            str = str.substring(0, str.length() - 1);

        Log4jLogFormat format = new Log4jLogFormat(pattern);

        LogRecord record = read(format, str);
        checkFields(record, fields);
    }

    private void check1(String pattern, LogEvent event, String ... fields) {
        check(pattern, event, fields);

        org.apache.log4j.PatternLayout patternLayout1 = new org.apache.log4j.PatternLayout(pattern);

        String str = patternLayout1.format(new LoggingEvent(event.getLoggerFqcn(),
                Logger.getLogger(event.getLoggerName()),
                event.getTimeMillis(),
                org.apache.log4j.Level.toLevel(event.getLevel().name()), event.getMessage(), event.getThreadName(), null,
                "ndc", new LocationInfo("Log4jLogFormatTest.java", "Log4jLogFormatTest", "testLog4j", "35"), null));

        if (str.endsWith("\n"))
            str = str.substring(0, str.length() - 1);

        Log4jLogFormat format = new Log4jLogFormat(pattern);

        LogRecord record = read(format, str);
        checkFields(record, fields);
    }

    private void notMatch(String pattern, String line) {
        Log4jLogFormat format = new Log4jLogFormat(pattern);

        LogReader reader = format.createReader();
        boolean isSuccess = reader.parseRecord(new BufferedFile.Line(line));
        assertFalse(isSuccess);
    }
}
