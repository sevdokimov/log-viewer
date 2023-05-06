package com.logviewer.data2;

import com.google.common.collect.Iterables;
import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.RegexLogFormat.RegexField;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.logviewer.TestUtils.assertUnparsed;
import static org.junit.Assert.assertEquals;

public class LogIterationForwardTest extends AbstractLogTest {

    public static final LogFormat FORMAT = new RegexLogFormat(
            "\\[(DEBUG|INFO)\\] (.+)",
            new RegexField("level", 1), new RegexField("body", 2));

    public static final LogFormat LINE_NUMBERS_NO_APPEND = new RegexLogFormat(
            "(?<msg>\\d+)", new RegexField("msg"))
            .setDontAppendUnmatchedTextToLastField(true);

    public static final LogFormat FORMAT_NO_APPEND = new RegexLogFormat(
            "\\[(DEBUG|INFO)\\] (.+)",
            new RegexField("level", 1), new RegexField("body", 2))
            .setDontAppendUnmatchedTextToLastField(true);

    @Test
    public void testEmpty() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/empty.log", FORMAT)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            assert res.isEmpty();

            assert log.processRecords(0, true, res::add);
            assert res.isEmpty();
        }
    }

    @Test
    public void testNewLineOnly() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/new-line-only.log", LogService.DEFAULT_FORMAT)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, false, res::add);
            assert res.size() == 2;
            assert res.stream().noneMatch(r -> r.getMessage().length() > 0);

            res.clear();
            assert log.processRecords(0, true, res::add);
            LogRecord record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(Collections.emptySet(), record.getFieldNames());

            res.clear();
            assert log.processRecords(1, false, res::add);
            record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(Collections.emptySet(), record.getFieldNames());

            res.clear();
            assert log.processRecords(1, true, res::add);
            assert res.isEmpty();
        }
    }

    @Test
    public void testSingleLine() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/single-line.log", FORMAT)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            LogRecord rec = Iterables.getOnlyElement(res);

            assertEquals("[DEBUG] l1", rec.getMessage());
            assertEquals(0, rec.getStart());
            assertEquals(rec.getMessage().length(), rec.getEnd());
            assert !rec.hasMore();

            assertEquals("DEBUG", rec.getFieldText("level"));
            assertEquals("l1", rec.getFieldText("body"));

            res.clear();
            assert log.processRecords(log.getSize(), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));

            log.processRecords(log.getSize(), true, t -> {throw new RuntimeException();});
        }
    }

    @Test
    public void test1() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/test1.log", FORMAT)) {
            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(allContent.indexOf("l3"), res::add);

            assertEquals(3, res.size());
            assertEquals("[DEBUG] l1\nl2\nl3", res.get(0).getMessage());
            assertEquals("[INFO] i1", res.get(1).getMessage());
            assertEquals("[INFO] i2", res.get(2).getMessage());

            res.clear();
            assert log.processRecords(allContent.indexOf("l3") + 2, true, res::add);
            assertEquals(2, res.size());
            assertEquals("[INFO] i1", res.get(0).getMessage());
        }
    }

    @Test
    public void startFromUnparsed() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/test2.log", LINE_NUMBERS_NO_APPEND)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(10, false, res::add); // from first unparsed line
            assertEquals(Arrays.asList("unparsed1\nunparsed2\nunparsed3", "1234567890"), res.stream().map(r -> r.getMessage()).collect(Collectors.toList()));
            res.clear();

            assert log.processRecords(20, false, res::add); // from first unparsed line
            assertEquals(Arrays.asList("unparsed1\nunparsed2\nunparsed3", "1234567890"), res.stream().map(r -> r.getMessage()).collect(Collectors.toList()));
            res.clear();

            assert log.processRecords(39, false, res::add); // from first unparsed line
            assertEquals(Arrays.asList("unparsed1\nunparsed2\nunparsed3", "1234567890"), res.stream().map(r -> r.getMessage()).collect(Collectors.toList()));
            res.clear();

            assert log.processRecords(39, true, res::add); // from first unparsed line
            assertEquals(Arrays.asList("1234567890"), res.stream().map(r -> r.getMessage()).collect(Collectors.toList()));
            res.clear();
        }
    }

    @Test
    public void testSingleLineTail() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/single-line-tail.log", FORMAT)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            LogRecord rec = Iterables.getOnlyElement(res);

            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            assertEquals(allContent, rec.getMessage());

            res.clear();
            assert log.processRecords(allContent.length(), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));

            res.clear();
            assert log.processRecords(allContent.indexOf("l2"), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));

            res.clear();
            assert log.processRecords(allContent.indexOf("l3"), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));

            res.clear();
            assert log.processRecords(allContent.indexOf("l4"), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));
        }
    }

    @Test
    public void testNoFirstRecord() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/no-first-record.log", FORMAT)) {
            List<LogRecord> res = new ArrayList<>();

            assert log.processRecords(0, res::add);

            assertUnparsed(res.get(0), "not-a-record\nnot-a-record2");
            assertEquals("[DEBUG] l1\nl2", res.get(1).getMessage());
            assertEquals("[DEBUG] zzz", res.get(2).getMessage());

            List<LogRecord> res2 = new ArrayList<>();
            assert log.processRecords(5, res2::add);
            TestUtils.assertEquals(res2, res);

            res2.clear();
            assert log.processRecords("not-a-record\nnot-a-record2".length(), res2::add);
            TestUtils.assertEquals(res2, res);
        }
    }

}
