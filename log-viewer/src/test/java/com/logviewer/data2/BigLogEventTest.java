package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.utils.TextRange;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BigLogEventTest extends AbstractLogTest {

    @Test
    public void testLog4JFormat() throws IOException {
        Log4jLogFormat format = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss} %msg%n");

        forward(format);
        backward(format);
        scanFromMiddleOfBigEvent(format);
    }

    @Test
    public void testLogback() throws IOException {
        LogbackLogFormat format = new LogbackLogFormat("%d{yyyy-MM-dd HH:mm:ss} %msg%n");

        forward(format);
        backward(format);
        scanFromMiddleOfBigEvent(format);
    }

    @Test
    public void testRegexpLogFormat() throws IOException {
        RegexLogFormat format = new RegexLogFormat(
                "(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d) (.*)",
                "yyyy-MM-dd HH:mm:ss", "date",
                new RegexLogFormat.RegexField("date", 1, "date"),
                new RegexLogFormat.RegexField("msg", 2, "message"));

        forward(format);
        backward(format);
        scanFromMiddleOfBigEvent(format);
    }

    private void forward(LogFormat format) throws IOException {
        try (Snapshot log = log("/testdata/big-log-event.log", format)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, res::add);

            checkMessages(res, log.getLog().getFile());
        }
    }

    private void backward(LogFormat format) throws IOException {
        try (Snapshot log = log("/testdata/big-log-event.log", format)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecordsBack(log.getSize(), false, res::add);

            Collections.reverse(res);
            
            checkMessages(res, log.getLog().getFile());
        }
    }

    private void scanFromMiddleOfBigEvent(LogFormat format) throws IOException {
        try (Snapshot log = log("/testdata/big-log-event.log", format)) {
            List<LogRecord> records = new ArrayList<>();

            assert log.processRecordsBack(log.getSize(), false, records::add);

            LogRecord bigMessage = records.get(2);

            assertThat(bigMessage.getMessage().length()).isGreaterThanOrEqualTo(ParserConfig.MAX_LINE_LENGTH);

            long middleOfBigEvent = bigMessage.getStart() + bigMessage.getMessage().length() / 2;

            List<LogRecord> recordsUp = new ArrayList<>();
            log.processRecordsBack(middleOfBigEvent, false, recordsUp::add);
            assertThat(recordsUp).hasSize(3);

            List<LogRecord> recordsDown = new ArrayList<>();
            log.processRecords(middleOfBigEvent, false, recordsDown::add);
            assertThat(recordsDown).hasSize(3);

            assertEquals(bigMessage, recordsDown.get(0));
            assertEquals(recordsUp.get(0), recordsDown.get(0));
        }
    }

    private static void checkMessages(List<LogRecord> events, Path file) throws IOException {
        assertThat(events).hasSize(5);

        for (int i = 0; i < events.size() - 1; i++) {
            assertEquals(events.get(i).getEnd() + 1, events.get(i + 1).getStart());
        }

        assertEquals(Files.size(file), events.get(4).getEnd());

        assertThat(events.get(0).getFieldText("msg")).isEqualTo("000");
        assertThat(events.get(1).getFieldText("msg")).isEqualTo("111");
        assertThat(events.get(3).getFieldText("msg")).isEqualTo("333");
        assertThat(events.get(4).getFieldText("msg")).isEqualTo("444");

        checkBigEvent(events.get(2), file);
    }

    private static void checkBigEvent(LogRecord bigEvent, Path file) throws IOException {
        assertThat(bigEvent.hasMore()).isTrue();
        assertThat(bigEvent.getMessage().length()).isLessThanOrEqualTo(ParserConfig.MAX_LINE_LENGTH + 100); // 100 - length of the first line

        TextRange msgTextRange = bigEvent.getFieldOffset("msg");
        assertEquals(bigEvent.getFieldOffset("date").getEnd() + 1, msgTextRange.getStart());
        assertEquals(bigEvent.getMessage().length(), msgTextRange.getEnd());

        assertThat(bigEvent.getFieldText("msg")).startsWith("222 ");

        byte[] bytes = Files.readAllBytes(file);

        assertEquals(bigEvent.getMessage(), new String(bytes, (int)bigEvent.getStart(), bigEvent.getMessage().length()));
    }



}
