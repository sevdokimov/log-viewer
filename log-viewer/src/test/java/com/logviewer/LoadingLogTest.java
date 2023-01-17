package com.logviewer;

import com.logviewer.data2.DirectoryNotVisibleException;
import com.logviewer.data2.Log;
import com.logviewer.data2.LogRecord;
import com.logviewer.data2.Snapshot;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.utils.Utils;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoadingLogTest extends AbstractLogTest {

    @Test
    public void testFormatModification() throws IOException {
        LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

        String logPath = getTestLog("multilog/search.log");

        Log log = getLogService().openLog(logPath, format);

        format.setPattern("%l");

        assertEquals("%d{ddMMyy HH:mm:ss} %m", ((LogbackLogFormat)log.getFormat()).getPattern());

        try (Snapshot snapshot = log.createSnapshot()) {
            List<LogRecord> list = new ArrayList<>();
            snapshot.processRecords(1, list::add);

            assertEquals("150101 10:00:01 zzz a", list.get(0).getMessage());
        }
    }

    @Test
    public void unpackGZipError() {
        LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

        String logPath = getTestLog("gz/search.log.gz");

        Log log = getLogService().openLog(logPath, format);

        try (Snapshot snapshot = log.createSnapshot()) {
            IOException ioException = TestUtils.assertError(IOException.class, () -> snapshot.processRecords(0, a -> true));
            assertTrue(ioException.getMessage().contains(Log.UNPACK_GZ_ARCHIVES));
        }
    }

    @Test
    public void unpackGZipOk() throws IOException {
        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

            String logPath = getTestLog("gz/search.log.gz");

            Log log = getLogService().openLog(logPath, format);

            try (Snapshot snapshot = log.createSnapshot()) {
                List<LogRecord> list = new ArrayList<>();
                snapshot.processRecords(0, list::add);

                assertTrue(list.size() > 0);
            }
        });

    }

    @Test
    public void unpackGZipCheckAccess() throws IOException {
        for (Path tmpFile : Files.list(Utils.getTempDir()).collect(Collectors.toList())) {
            FileSystemUtils.deleteRecursively(tmpFile);
        }

        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

            String logPath = getTestLog("gz/search.log.gz");

            getCommonContext().getBean(LvFileAccessManagerImpl.class).setVisibleFiles(Collections.singletonList(Paths.get(logPath)));

            Log logDenied = getLogService().openLog(getTestLog("log.log"));
            try (Snapshot snapshot = logDenied.createSnapshot()) {
                TestUtils.assertError(DirectoryNotVisibleException.class, () -> snapshot.processRecords(0, a -> true));
            }

            Log logGzip = getLogService().openLog(logPath, format);

            for (int i = 0; i < 2; i++) { // this test checks the error that occurs second times only
                try (Snapshot snapshot = logGzip.createSnapshot()) {
                    List<LogRecord> list = new ArrayList<>();
                    snapshot.processRecords(0, list::add);

                    assertTrue(list.size() > 0);
                }
            }

            long fileCount = Files.list(Utils.getTempDir())
                    .filter(f -> f.getFileName().toString().startsWith("unpacked-gz"))
                    .count();

            assertEquals(1L, fileCount);
        });
    }

}
