package com.logviewer;

import com.logviewer.data2.DirectoryNotVisibleException;
import com.logviewer.data2.Log;
import com.logviewer.data2.LogRecord;
import com.logviewer.data2.Snapshot;
import com.logviewer.formats.SimpleLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.utils.Utils;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class LoadingLogTest extends AbstractLogTest {

    @Test
    public void testFormatModification() throws IOException {
        LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");
        format.setCharset(StandardCharsets.ISO_8859_1);

        String logPath = getTestLog("multilog/search.log");

        Log log = getLogService().openLog(logPath, format);

        format.setCharset(StandardCharsets.UTF_8);

        assertEquals(StandardCharsets.ISO_8859_1, log.getFormat().getCharset()); // the log object contains a copy of the format

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
        clearTempDirectory();

        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

            String logInGz = getTestLog("gz/search.log.gz");
            String plainLog = getTestLog("log.log");

            getCommonContext().getBean(LvFileAccessManagerImpl.class).setVisibleFiles(Collections.singletonList(Paths.get(plainLog)));

            Log gzLog = getLogService().openLog(logInGz);
            try (Snapshot snapshot = gzLog.createSnapshot()) {
                TestUtils.assertError(DirectoryNotVisibleException.class, () -> snapshot.processRecords(0, a -> true));
            }

            getCommonContext().getBean(LvFileAccessManagerImpl.class).setVisibleFiles(Collections.singletonList(Paths.get(logInGz)));

            for (int i = 0; i < 2; i++) { // this test checks the error that occurs second times only
                try (Snapshot snapshot = gzLog.createSnapshot()) {
                    List<LogRecord> list = new ArrayList<>();
                    snapshot.processRecords(0, list::add);

                    assertTrue(list.size() > 0);
                }
            }
        });
    }

    @Test
    public void unpackZipOk() throws IOException {
        clearTempDirectory();

        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            LogbackLogFormat format = new LogbackLogFormat("%d{ddMMyy HH:mm:ss} %m");

            String logPath = getTestLog("gz/search.zip");

            long fileSize;
            try (ZipFile zip = new ZipFile(logPath)) {
                fileSize = zip.getEntry("search.log").getSize();
            }

            Log log = getLogService().openLog(logPath, format);

            try (Snapshot snapshot = log.createSnapshot()) {
                assertEquals(fileSize, snapshot.getSize());

                List<LogRecord> list = new ArrayList<>();
                snapshot.processRecords(0, list::add);

                assertTrue(list.size() > 0);
            }
        });
    }

    @Test
    public void brokenZipArchive() {
        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            try (Snapshot snapshot = log("/testdata/gz/broken-archive.zip", null)) {
                assertThat(snapshot.getError()).isInstanceOf(IOException.class);
            }
        });
    }

    @Test
    public void moreThanOneItemInZip() {
        TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
            try (Snapshot snapshot = log("/testdata/gz/more-than-one-item.zip", null)) {
                assertThat(snapshot.getError()).isInstanceOf(IOException.class);
                assertThat(snapshot.getError().getMessage()).contains("more than one file");
            }
        });
    }

    @Test
    public void overrideArchive() throws IOException {
        Path tmpLogFile = Utils.getTempDir().resolve("anArchive.zip");

        Files.copy(Paths.get(getTestLog("gz/search.zip")), tmpLogFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        try {
            TestUtils.withSystemProp(Log.UNPACK_GZ_ARCHIVES, "true", () -> {
                Log log = getLogService().openLog(tmpLogFile, new SimpleLogFormat());

                List<LogRecord> list1 = new ArrayList<>();
                long size1;
                String hash1;
                List<LogRecord> list2 = new ArrayList<>();

                try (Snapshot snapshot = log.createSnapshot()) {
                    snapshot.processRecords(1, list1::add);

                    assertThat(list1).hasSizeGreaterThan(2);

                    size1 = snapshot.getSize();
                    hash1 = snapshot.getHash();
                }

                try (ZipOutputStream zOut = new ZipOutputStream(Files.newOutputStream(tmpLogFile))) {
                    zOut.putNextEntry(new ZipEntry("a.log"));
                    zOut.write("aaa".getBytes(StandardCharsets.UTF_8));
                }

                try (Snapshot snapshot = log.createSnapshot()) {
                    snapshot.processRecords(1, list2::add);

                    assertThat(list2).hasSize(1);

                    assertNotEquals(size1, snapshot.getSize());
                    assertThat(snapshot.isValidHash(hash1)).isFalse();
                    assertThat(snapshot.isValidHash(snapshot.getHash())).isTrue();
                }
            });
        } finally {
            Files.deleteIfExists(tmpLogFile);
        }
    }

    private static void clearTempDirectory() throws IOException {
        for (Path tmpFile : Files.list(Utils.getTempDir()).collect(Collectors.toList())) {
            FileSystemUtils.deleteRecursively(tmpFile);
        }
    }
}
