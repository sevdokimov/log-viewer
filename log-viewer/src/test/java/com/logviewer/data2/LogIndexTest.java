package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.logviewer.TestUtils.MULTIFILE_LOG_FORMAT;
import static com.logviewer.TestUtils.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LogIndexTest extends AbstractLogTest {

    @Test
    public void noTimedRecord() throws IOException, LogCrashedException, URISyntaxException {
        try (Snapshot snapshot = log("/testdata/date/log-with-time.log", MULTIFILE_LOG_FORMAT)) {
            LogIndex index = new LogIndex();
            assertNull(index.findRecordBound(0, false, snapshot));
            assertNull(index.findRecordBound(snapshot.getSize(), false, snapshot));
            assertNull(index.findRecordBound(snapshot.getSize() + 100, false, snapshot));
            assertNull(index.findRecordBound(5, false, snapshot));
        }
    }

    @Test
    public void search1() throws IOException, LogCrashedException, URISyntaxException {
        try (Snapshot snapshot = log("/testdata/date/log-with-time-2.log", MULTIFILE_LOG_FORMAT)) {
            LogIndex index = new LogIndex();
            
            check(0, index.findRecordBound(date(0, 0), false, snapshot));
            assertNull(index.findRecordBound(date(0, 0), true, snapshot));

            check(0, index.findRecordBound(date(0, 1), false, snapshot));
            check(0, index.findRecordBound(date(0, 1), true, snapshot));

            check(18, index.findRecordBound(date(0, 2), false, snapshot));
            check(0, index.findRecordBound(date(0, 2), true, snapshot));

            check(18, index.findRecordBound(date(0, 3), false, snapshot));
            check(18 * 3, index.findRecordBound(date(0, 3), true, snapshot));

            check(18 * 4, index.findRecordBound(date(0, 4), false, snapshot));
            check(18 * 3, index.findRecordBound(date(0, 4), true, snapshot));

            check(18 * 4, index.findRecordBound(date(0, 5), false, snapshot));
            check(18 * 4, index.findRecordBound(date(0, 5), true, snapshot));

            assertNull(index.findRecordBound(date(0, 9), false, snapshot));
            check(18 * 4, index.findRecordBound(date(0, 9), true, snapshot));
        }
    }

    @Test
    public void searchLong() throws IOException, LogCrashedException {
        Path tempFile = createTempFile();

        List<String> content = new ArrayList<>();
        content.addAll(Arrays.asList("150101 10:00:01 a", "150101 10:00:03 b", "150101 10:00:03 c", "150101 10:00:03 d", "150101 10:00:05 e"));
        content.addAll(Collections.nCopies(300, "150101 10:00:08 c"));

        Files.write(tempFile, content);

        try (Snapshot snapshot = log(tempFile, MULTIFILE_LOG_FORMAT)) {
            LogIndex index = new LogIndex();

            check(0, index.findRecordBound(date(0, 0), false, snapshot));
            assertNull(index.findRecordBound(date(0, 0), true, snapshot));

            check(0, index.findRecordBound(date(0, 1), false, snapshot));
            check(0, index.findRecordBound(date(0, 1), true, snapshot));

            check(18, index.findRecordBound(date(0, 2), false, snapshot));
            check(0, index.findRecordBound(date(0, 2), true, snapshot));

            check(18, index.findRecordBound(date(0, 3), false, snapshot));
            check(18 * 3, index.findRecordBound(date(0, 3), true, snapshot));

            check(18 * 4, index.findRecordBound(date(0, 4), false, snapshot));
            check(18 * 3, index.findRecordBound(date(0, 4), true, snapshot));

            check(18 * 4, index.findRecordBound(date(0, 5), false, snapshot));
            check(18 * 4, index.findRecordBound(date(0, 5), true, snapshot));

            assertNull(index.findRecordBound(date(0, 9), false, snapshot));
            check(18 * 304, index.findRecordBound(date(0, 9), true, snapshot));
        }
    }

    @Test
    public void textBeforeStart() throws IOException, LogCrashedException {
        Path tempFile = createTempFile();

        Files.write(tempFile, Arrays.asList("1111111zzzZZZzzzR", "150101 10:00:01 a", "150101 10:00:03 b", "150101 10:00:03 c", "150101 10:00:03 d", "150101 10:00:05 e"));

        try (Snapshot snapshot = log(tempFile, MULTIFILE_LOG_FORMAT)) {
            LogIndex index = new LogIndex();
            check(18, index.findRecordBound(date(0, 0), false, snapshot));
            assertNull(index.findRecordBound(date(0, 0), true, snapshot));

            check(18, index.findRecordBound(date(0, 1), false, snapshot));
            check(18, index.findRecordBound(date(0, 1), true, snapshot));
        }
    }

    @Test
    public void iterateFromTime() throws IOException, LogCrashedException, URISyntaxException {
        try (Snapshot snapshot = log("/testdata/date/log-with-time-2.log", MULTIFILE_LOG_FORMAT)) {
            List<Record> res = new ArrayList<>();

            boolean f = snapshot.processFromTime(date(0, 2), res::add);
            assert f;
            TestUtils.check(res, "150101 10:00:03 b", "150101 10:00:03 c", "150101 10:00:03 d", "150101 10:00:05 e");

            res.clear();
            f = snapshot.processFromTime(date(0, 9), res::add);
            assert f;
            assert res.isEmpty();
        }
    }

    @Test
    public void iterateFromTimeBack() throws IOException, LogCrashedException, URISyntaxException {
        try (Snapshot snapshot = log("/testdata/date/log-with-time-2.log", MULTIFILE_LOG_FORMAT)) {
            List<Record> res = new ArrayList<>();

            boolean f = snapshot.processFromTimeBack(date(0, 3), res::add);
            assert f;
            TestUtils.check(res, "150101 10:00:03 d", "150101 10:00:03 c", "150101 10:00:03 b", "150101 10:00:01 a");

            res.clear();
            f = snapshot.processFromTimeBack(date(0, 0), res::add);
            assert f;
            assert res.isEmpty();
        }
    }

    private void check(long expectedPos, Record record) {
        assertEquals(expectedPos, record.getStart());
    }

}
