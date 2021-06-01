package com.logviewer.perfomance;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.*;
import com.logviewer.filters.JsPredicate;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 *
 */
@Ignore
public class PerformanceTest extends AbstractLogTest {

    private static final LogFormat FORMAT_REGEX = new RegexLogFormat(StandardCharsets.UTF_8,
            "((?:19|20)\\d\\d-(?:0[1-9]|1[012])-(?:0[1-9]|[12]\\d|3[01])_(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d\\d\\d) \\[(.+?)] (OFF|ERROR|WARN|INFO|DEBUG|TRACE|ALL) {0,5}? ((?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*) - (.*)",
            false,
            "yyyy-MM-dd_HH:mm:ss.SSS", "date",
            new RegexLogFormat.RegexField("date", 1, "date"),
            new RegexLogFormat.RegexField("thread", 2, "thread"),
            new RegexLogFormat.RegexField("level", 3, "level/logback"),
            new RegexLogFormat.RegexField("logger", 4, "class"),
            new RegexLogFormat.RegexField("msg", 5, "message")
            );

    private static final LogFormat FORMAT_LOGBACK = new LogbackLogFormat(StandardCharsets.UTF_8,
            "%date{yyyy-MM-dd_HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %X{pipelineId}%X{contentId}%msg%n");

    @Test
    @Ignore
    public void testJs() throws IOException {
        doPredicateTest(new JsPredicate("level == 'ERROR' || level == 'WARN'"));
    }

    @Test
    @Ignore
    public void testRead() throws IOException {
        Log log = createLog();

        System.out.println("Warmup");

        long startTime = System.currentTimeMillis();

        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, r -> {
                return true;
            });
        }

        System.out.println("Warmup done: " + (System.currentTimeMillis() - startTime));

        System.out.println("Test execution...");

        startTime = System.currentTimeMillis();

        int[] lineCounter = new int[1];

        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, r -> {
                lineCounter[0]++;
                return true;
            });
        }

        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println("Time: " + executionTime
                + ", Line / second: " + (lineCounter[0] / (executionTime / 1000d))
                + ", mb / second: " + ((Files.size(log.getFile()) / (1024*1024d)) / (executionTime / 1000d))
        );

        System.out.println("Lines: " + lineCounter[0]);
    }

    private Log createLog() {
        String canonicalPath = "/home/sevdokimov/logs/dashboard.log";

        return getLogService().openLog(canonicalPath, FORMAT_LOGBACK);
    }

    private void doPredicateTest(RecordPredicate predicate) throws IOException {
        Log log = createLog();
        LvPredicateChecker filterContext = new LvPredicateChecker(log);

        System.out.println("Warmup");

        LogRecord[] rrr = new LogRecord[1];

        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, r -> {
                rrr[0] = r;
                return true;
            });

            for (int i = 0; i < 150_000; i++) {
                predicate.test(rrr[0], filterContext);
            }

            System.out.println("Recording");

            int[] lineCounter = new int[2];

            long startTime = System.currentTimeMillis();

            snapshot.processRecords(0, r -> {
                lineCounter[0]++;

                if (predicate.test(r, filterContext))
                    lineCounter[1]++;

                return true;
            });

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Time: " + executionTime + ", Line per second: " + (lineCounter[0] / (executionTime / 1000d)));
            System.out.println("Lines: " + lineCounter[0]);
        }

    }
}
