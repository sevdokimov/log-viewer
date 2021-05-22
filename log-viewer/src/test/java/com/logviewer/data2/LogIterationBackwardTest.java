package com.logviewer.data2;

import com.google.common.collect.Iterables;
import com.logviewer.AbstractLogTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.logviewer.TestUtils.check;
import static org.junit.Assert.assertEquals;

public class LogIterationBackwardTest extends AbstractLogTest {

    @Test
    public void testEmpty() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/empty.log", LogIterationForwardTest.FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(0, false, res::add);

            assert res.isEmpty();

            res.clear();
            assert log.processRecordsBack(0, true, res::add);
            assert res.size() == 0;
        }
    }

    @Test
    public void testNewLineOnly() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/new-line-only.log", LogService.DEFAULT_FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(0, false, res::add);
            Record record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(0, record.getFieldsCount());

            res.clear();
            assert log.processRecordsBack(0, true, res::add);
            assert res.isEmpty();

            res.clear();
            assert log.processRecordsBack(1, false, res::add);
            assert res.size() == 2;
            assert res.stream().noneMatch(r -> r.getMessage().length() > 0);

            res.clear();
            assert log.processRecordsBack(1, true, res::add);
            record = Iterables.getOnlyElement(res);
            assertEquals("", record.getMessage());
            assertEquals(0, record.getFieldsCount());
        }
    }

    @Test
    public void testTest1NoAppend() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/test1.log", LogIterationForwardTest.FORMAT_NO_APPEND)) {
            List<Record> res = new ArrayList<>();

            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            assert log.processRecordsBack(allContent.indexOf("l2"), false, res::add);
            check(res, "l2\nl3", "[DEBUG] l1", "[DEBUG] l0");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l3"), false, res::add);
            check(res, "l2\nl3", "[DEBUG] l1", "[DEBUG] l0");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("[INFO] i1"), false, res::add);
            check(res, "[INFO] i1", "l2\nl3", "[DEBUG] l1", "[DEBUG] l0");
        }
    }

    @Test
    public void testTest1() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/test1.log", LogIterationForwardTest.FORMAT)) {
            List<Record> res = new ArrayList<>();

            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            assert log.processRecordsBack(allContent.indexOf("l2"), false, res::add);
            check(res, "[DEBUG] l1\nl2\nl3", "[DEBUG] l0");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l3"), false, res::add);
            check(res, "[DEBUG] l1\nl2\nl3", "[DEBUG] l0");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("[INFO] i1"), false, res::add);
            check(res, "[INFO] i1", "[DEBUG] l1\nl2\nl3", "[DEBUG] l0");
        }
    }

    @Test
    public void testSingleLine() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/single-line.log", LogIterationForwardTest.FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(0, false, res::add);
            check(res, "[DEBUG] l1");

            res.clear();
            assert log.processRecordsBack(0, true, res::add);
            check(res);

            res.clear();
            assert log.processRecordsBack(log.getSize(), false, res::add);
            check(res, "[DEBUG] l1");
        }
    }

    @Test
    public void testSingleLineTail() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/single-line-tail.log", LogIterationForwardTest.FORMAT)) {
            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(0, true, res::add);
            assert res.size() == 0;

            for (int i = 0; i < log.getSize(); i++) {
                res.clear();
                assert log.processRecordsBack(i, false, res::add);
                check(res, "[DEBUG] l1\nl2\nl3\nl4");
            }
        }
    }

    @Test
    public void testSingleLineTailNoAppend() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/single-line-tail.log", LogIterationForwardTest.FORMAT_NO_APPEND)) {
            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(log.getSize(), false, res::add);
            check(res, "l2\nl3\nl4", "[DEBUG] l1");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l4"), false, res::add);
            check(res, "l2\nl3\nl4", "[DEBUG] l1");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l2"), false, res::add);
            check(res, "l2\nl3\nl4", "[DEBUG] l1");

            res.clear();
            assert log.processRecordsBack(0, false, res::add);
            check(res, "[DEBUG] l1");
        }
    }

    @Test
    public void noFirstRecordNoAppend() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/no-first-record.log", LogIterationForwardTest.FORMAT_NO_APPEND)) {
            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(allContent.indexOf("not-a-record2"), false, res::add);
            check(res, "not-a-record\nnot-a-record2");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l2"), false, res::add);
            check(res, "l2", "[DEBUG] l1", "not-a-record\nnot-a-record2");
        }
    }

    @Test
    public void noLastRecord() throws IOException {
        try (Snapshot log = log("/testdata/log-iteration/no-first-record.log", LogIterationForwardTest.FORMAT)) {
            String allContent = new String(Files.readAllBytes(log.getLog().getFile()));

            List<Record> res = new ArrayList<>();

            assert log.processRecordsBack(allContent.indexOf("not-a-record2"), false, res::add);
            check(res, "not-a-record\nnot-a-record2");

            res.clear();
            assert log.processRecordsBack(allContent.indexOf("l2"), false, res::add);
            check(res, "[DEBUG] l1\nl2", "not-a-record\nnot-a-record2");
        }
    }

}
