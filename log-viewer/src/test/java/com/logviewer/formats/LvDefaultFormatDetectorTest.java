package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LvDefaultFormatDetectorTest extends AbstractLogTest {

    @Test
    public void testDateLevel() {
        checkLine("2020-01-01 18:11:00,000 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} %level %m%n");
        checkLine("2020-01-01 18:11:00,000      WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} %level %m%n");
        checkLine("2020-01-01 18:11:00,000 [     WARN] ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("2020-01-01 18:11:00,000 [WARN   ] ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("2020-01-01 18:11:00,000 [WARN   ]    ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("[2020-01-01 18:11:00,000] WARN ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level %m%n");
        checkLine("[2020-01-01 18:11:00,000]      WARN ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level %m%n");
        checkLine("[2020-01-01 18:11:00,000] [     WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");
        checkLine("[2020-01-01 18:11:00,000] [WARN   ] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");
        checkLine("[2020-01-01 18:11:00,000] [WARN   ]    ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");

        checkLine("[2020-01-01 18:11:00,000][WARN   ]    ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}][%level] %m%n");

        checkLine("[2017-11-23T14:33:24,328][INFO ][o.e.c.m.MetaDataMappingService] [G1WVvSO] [behavox_tes] create_mapping", "[%d{yyyy-MM-dd HH:mm:ss.SSS}][%level]%m%n");

        checkLine("[2020-01-01 18:11:00,000]   [WARN] ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("[2020-01-01 18:11:00,000] [WARN]:ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine(" [2020-01-01 18:11:00,000] WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00,000 z WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
    }

    @Test
    public void testDateThreadLevel() {
        checkLine("2020-01-01 18:11:00,000 [my-thread] WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %m%n");
        checkLine("2020-01-01 18:11:00,000 [my-thread]     WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %m%n");
        checkLine("2020-01-01 18:11:00,000 [my-thread-01]     WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %m%n");
        checkLine("[2020-01-01 18:11:00,000][my-thread][WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}][%t][%level] %m%n");
        checkLine("[2020-01-01 18:11:00,000][my-thread][WARN][zzzz] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}][%t][%level]%m%n");
        checkLine("[2020-01-01 18:11:00,000] [my-thread] [WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%t] [%level] %m%n");

        checkLine("[2020-01-01 18:11:00,000] my-thread [WARN] ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00,000 [my-thread]     [WARN] ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
    }

    @Test
    public void testLevelDate() {
        checkLine("WARN 2020-01-01 18:11:00.000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("WARN 2020-01-01 18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("WARN 2020-01-01_18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("WARN 2020-01-01T18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("WARN [2020-01-01T18:11:00,000] mmm", "%level [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
        checkLine("[WARN] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("[WARN   ] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("[  WARN] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("[WARN] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
        checkLine("[WARN][2020-01-01 18:11:00,000][zzz] mmm", "[%level][%d{yyyy-MM-dd HH:mm:ss.SSS}]%m%n");
        checkLine("[WARN  ] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
        checkLine("[  WARN] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
        checkLine("[  WARN][2020-01-01 18:11:00,000] mmm", "[%level][%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
        checkLine("WARN   2020-01-01 18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");

        checkLine("WARN 2020-01-01 18:11:00,000mmm", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("WARN 2020-01-01 18:11:00,000:mmm", LvDefaultFormatDetector.UNKNOWN_FORMAT);

        checkLine("[WARN]   [2020-01-01 18:11:00,000] mmm", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("xxx WARN 2020-01-01 18:11:00,000 mmm", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("xxx 2020-01-01 18:11:00,000 mmm", LvDefaultFormatDetector.UNKNOWN_FORMAT);
    }

    @Test
    public void detectByPath() {
        Log4jLogFormat format = detect("/testdata/format-detection/level-date.log");
        assertEquals("%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n", format.getPattern());
    }

    @Test
    public void stacktrace() {
        Log4jLogFormat format = detect("/testdata/format-detection/level-date-stacktrace.log");
        assertEquals("%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n", format.getPattern());
    }

    @Test
    public void ambiguousFormat() {
        Log4jLogFormat format = detect("/testdata/format-detection/ambiguous-format.log");
        assertNull(format);
    }

    @Test
    public void testUnknownFormat() {
        checkLine("13:12:10 fsdff", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("INFO fsdff", LvDefaultFormatDetector.UNKNOWN_FORMAT);
    }

    @Test
    public void testDateFormat() {
        checkLine("2020-01-01 18:11:00 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} %level %m%n");

        checkLine("20200101 181100 WARN ddddd", "%d{yyyyMMdd HHmmss} %level %m%n");
        checkLine("20200101181100 WARN ddddd", "%d{yyyyMMddHHmmss} %level %m%n");
        checkLine("20200101T181100 WARN ddddd", "%d{yyyyMMdd'T'HHmmss} %level %m%n");
        checkLine("20200101_181100 WARN ddddd", "%d{yyyyMMdd_HHmmss} %level %m%n");
        checkLine("20200101_181100.555 WARN ddddd", "%d{yyyyMMdd_HHmmss.SSS} %level %m%n");
        checkLine("20200101_181100,555 WARN ddddd", "%d{yyyyMMdd_HHmmss,SSS} %level %m%n");

        checkLine("INFO 20200101_181100,555 ddddd", "%level %d{yyyyMMdd_HHmmss,SSS} %m%n");
        checkLine("INFO 20200101181100555 ddddd", "%level %d{yyyyMMddHHmmssSSS} %m%n");

        checkLine("INFO 26 Sep 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Aug 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Oct 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Nov 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO [26 Dec 2020 10:35:02,625] ddddd", "%level [%d{dd MMM yyyy HH:mm:ss,SSS}] %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59,000 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59.000 ddddd", "%level %d{dd MMM yyyy HH:mm:ss.SSS} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59 ddddd", "%level %d{dd MMM yyyy HH:mm:ss} %m%n");
        checkLine("INFO 01 Dec 2018 00:00:00 ddddd", "%level %d{dd MMM yyyy HH:mm:ss} %m%n");
    }

    private Log4jLogFormat detect(String resourceName) {
        URL url = getClass().getResource(resourceName);
        assert url.getProtocol().equals("file");

        Path path = Paths.get(url.getFile());
        return (Log4jLogFormat) LvDefaultFormatDetector.detectFormat(path);
    }

    private void checkLine(@Nonnull String line, @Nullable String format) {
        String detectedFormat = LvDefaultFormatDetector.detectFormatOfLine(line);
        assertEquals(format, detectedFormat);

        if (format == null || format.equals(LvDefaultFormatDetector.UNKNOWN_FORMAT))
            return;

        Log4jLogFormat f = new Log4jLogFormat(format);
        read(f, line);
    }

}