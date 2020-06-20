package com.logviewer.data2;

import com.google.common.collect.Iterables;
import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.RegexLogFormat.RegexpField;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.logviewer.TestUtils.assertUnparsed;
import static org.junit.Assert.assertEquals;

public class LogIterationForwardTest extends AbstractLogTest {

    public static final LogFormat FORMAT = new RegexLogFormat(StandardCharsets.UTF_8, "body",
            "\\[(DEBUG|INFO)\\] (.+)", new RegexpField("level", 1), new RegexpField("body", 2));

    public static final LogFormat FORMAT_NO_APPEND = new RegexLogFormat(StandardCharsets.UTF_8, null,
            "\\[(DEBUG|INFO)\\] (.+)", new RegexpField("level", 1), new RegexpField("body", 2));

    @Test
    public void testEmpty() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/empty.log", FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            assert res.isEmpty();

            assert log.processRecords(0, true, res::add);
            assert res.isEmpty();
        }
    }

    @Test
    public void testNewLineOnly() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/new-line-only.log", LogService.DEFAULT_FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecords(0, false, res::add);
            assert res.size() == 2;
            assert res.stream().noneMatch(r -> r.getMessage().length() > 0);

            res.clear();
            assert log.processRecords(0, true, res::add);
            Record record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(0, record.getFieldsCount());

            res.clear();
            assert log.processRecords(1, false, res::add);
            record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(0, record.getFieldsCount());

            res.clear();
            assert log.processRecords(1, true, res::add);
            assert res.isEmpty();
        }
    }

    @Test
    public void testSingleLine() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/single-line.log", FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            Record rec = Iterables.getOnlyElement(res);

            assertEquals("[DEBUG] l1", rec.getMessage());
            assertEquals(0, rec.getStart());
            assertEquals(rec.getMessage().length(), rec.getEnd());
            assert !rec.hasMore();

            assertEquals("DEBUG", rec.getFieldText(log.getLog().getFormat().getFieldIndexByName("level")));
            assertEquals("l1", rec.getFieldText(log.getLog().getFormat().getFieldIndexByName("body")));

            res.clear();
            assert log.processRecords(log.getSize(), res::add);
            TestUtils.assertEquals(rec, Iterables.getOnlyElement(res));

            log.processRecords(log.getSize(), true, t -> {throw new RuntimeException();});
        }
    }

    @Test
    public void test1() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/test1.log", FORMAT)) {
            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            List<Record> res = new ArrayList<>();

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
    public void testSingleLineTail() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/single-line-tail.log", FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecords(0, res::add);
            Record rec = Iterables.getOnlyElement(res);

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
    public void testNoFirstRecord() throws URISyntaxException, IOException, LogCrashedException {
        try (Snapshot log = log("/testdata/log-iteration/no-first-record.log", FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecords(0, res::add);

            assertUnparsed(res.get(0), "not-a-record\nnot-a-record2");
            assertEquals("[DEBUG] l1\nl2", res.get(1).getMessage());
            assertEquals("[DEBUG] zzz", res.get(2).getMessage());

            List<Record> res2 = new ArrayList<>();
            assert log.processRecords(5, res2::add);
            TestUtils.assertEquals(res2, res);

            res2.clear();
            assert log.processRecords("not-a-record\nnot-a-record2".length(), res2::add);
            TestUtils.assertEquals(res2, res);
        }
    }

}
