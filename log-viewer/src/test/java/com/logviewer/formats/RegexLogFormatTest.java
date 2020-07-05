package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        Record record = read(logFormat, "111 message text");

        assertEquals("111", record.getFieldText(logFormat.getFieldIndexByName("date")));
        assertEquals("message text", record.getFieldText(logFormat.getFieldIndexByName("msg")));
    }

    @Test
    public void appendTailDisabled() throws IOException, LogCrashedException {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>\\d+) (?<msg>.+)", true,
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
        );

        String logPath = getTestLog("LogParser/regex-log-parser.log");
        Log log = getLogService().openLog(logPath, logFormat);

        try (Snapshot snapshot = log.createSnapshot()) {
            List<Record> res = new ArrayList<>();

            snapshot.processRecords(0, res::add);

            assertEquals(Arrays.asList(
                    "111 aaa",
                    "a_ a_ a_\na__ a__ a__",
                    "222 bbb",
                    "b_ b_ b_"
                    ),
                    res.stream().map(Record::getMessage).collect(Collectors.toList()));
        }

    }

    @Test
    public void appendTail() throws IOException, LogCrashedException {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
                "(?<date>\\d+) (?<msg>.+)", false,
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
        );

        String logPath = getTestLog("LogParser/regex-log-parser.log");
        Log log = getLogService().openLog(logPath, logFormat);

        try (Snapshot snapshot = log.createSnapshot()) {
            List<Record> res = new ArrayList<>();

            snapshot.processRecords(0, res::add);

            assertEquals(Arrays.asList(
                    "111 aaa\na_ a_ a_\na__ a__ a__",
                    "222 bbb\nb_ b_ b_"
                    ),
                    res.stream().map(Record::getMessage).collect(Collectors.toList()));
        }

    }
}
