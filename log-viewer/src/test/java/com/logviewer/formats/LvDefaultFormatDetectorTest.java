package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.utils.Pair;
import org.junit.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LvDefaultFormatDetectorTest extends AbstractLogTest {

    @Test
    public void testSpringPattern() {
        checkLine("2021-06-24 10:29:22.662  INFO 5571 --- [pool-1-thread-5] com.logviewer.demo.LogGeneratorService   : msg", "%d{yyyy-MM-dd HH:mm:ss.SSS} %p %processId --- [%t] %logger : %m%n");
        checkLine("2021-06-24 10:29:25.416 ERROR 5571 --- [pool-1-thread-2] com.logviewer.demo.LogGeneratorService   : Failed to execute method", "%d{yyyy-MM-dd HH:mm:ss.SSS} %p %processId --- [%t] %logger : %m%n");
    }

    @Test
    public void testDateLevel() {
        checkLine("2020-01-01 18:11:00,000 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} %level %m%n");
        checkLine("2020-01-01 18:11:00.000      WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 [     WARN] ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("2020-01-01 18:11:00.000 [WARN   ] ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("2020-01-01 18:11:00.000 [WARN   ]    ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %m%n");
        checkLine("[2020-01-01 18:11:00.000] WARN ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level %m%n");
        checkLine("[2020-01-01 18:11:00.000]      WARN ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level %m%n");
        checkLine("[2020-01-01 18:11:00.000] [     WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");
        checkLine("[2020-01-01 18:11:00.000] [WARN   ] ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");
        checkLine("[2020-01-01 18:11:00.000] [WARN   ]    ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] %m%n");
        checkLine("[2020/01/01 18:11:00.000] [WARN   ]    ddddd", "[%d{yyyy/MM/dd HH:mm:ss.SSS}] [%level] %m%n");

        checkLine("[2020-01-01 18:11:00.000][WARN   ]    ddddd", "[%d{yyyy-MM-dd HH:mm:ss.SSS}][%level] %m%n");

        checkLine("[2020-Jan-01 18:11:00.000][WARN] ddddd", "[%d{yyyy-MMM-dd HH:mm:ss.SSS}][%level] %m%n");
        checkLine("[2020-Dec-01_18:11:00.000][WARN] ddddd", "[%d{yyyy-MMM-dd_HH:mm:ss.SSS}][%level] %m%n");
        checkLine("[2020 Jan 01 18:11:00.000][WARN] ddddd", "[%d{yyyy MMM dd HH:mm:ss.SSS}][%level] %m%n");

        checkLine("[2017-11-23T14:33:24,328][INFO ][o.e.c.m.MetaDataMappingService] [G1WVvSO] [behavox_tes] create_mapping", "[%d{yyyy-MM-dd'T'HH:mm:ss,SSS}][%level]%m%n");

        checkLine("[2020-01-01 18:11:00,000]   [WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("[2020-01-01 18:11:00,000] [WARN]:ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine(" [2020-01-01 18:11:00,000] WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00,000 z WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
        checkLine("[2021-10-20_15:40:15.032] [fullhouse,,,] [main] INFO  o.s.d.r.c.RepositoryConfigurationDelegate - Bootstrapping Spring Data JPA", "[%d{yyyy-MM-dd_HH:mm:ss.SSS}] %m%n");
    }

    @Test
    public void testDateThreadLevel() {
        checkLine("2020-01-01 18:11:00,000 [my-thread] WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %level %m%n");
        checkLine("2020-01-01 18:11:00,000 [my-thread]     WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %level %m%n");
        checkLine("2020-01-01 18:11:00,000 [my-thread-01]     WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %level %m%n");
        checkLine("[2020-01-01 18:11:00,000][my-thread][WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}][%t][%level] %m%n");
        checkLine("[2020-01-01 18:11:00,000][my-thread][WARN][zzzz] ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}][%t][%level]%m%n");
        checkLine("[2020-01-01 18:11:00,000] [my-thread] [WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}] [%t] [%level] %m%n");

        checkLine("[2020-Sep-01 18:11:00,000] [my-thread] [WARN] ddddd", "[%d{yyyy-MMM-dd HH:mm:ss,SSS}] [%t] [%level] %m%n");

        checkLine("[2020-01-01 18:11:00,000] my-thread [WARN] ddddd", "[%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("2020-01-01 18:11:00,000 [my-thread]     [WARN] ddddd", "%d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
    }

    @Test
    public void testDateOnly() {
        checkLine("2020-01-01 18:11:00.000 mmm", "%d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("[2020-01-01 18:11:00.000][zzz] mmm", "[%d{yyyy-MM-dd HH:mm:ss.SSS}]%m%n");
        checkLine("2020-01-01 18:11:00.000+06: mmm", "%d{yyyy-MM-dd HH:mm:ss.SSSz}: %m%n");
        checkLine("2020/04/17 19:08:43.561 18866 140483982022400 S+  Thread: SystemBusIO1", "%d{yyyy/MM/dd HH:mm:ss.SSS} %m%n");
        checkLine("2020-02-10T06:26:39.901Z 0 [Warning] InnoDB: New log files created, LSN=45790)", "%d{yyyy-MM-dd'T'HH:mm:ss.SSSz} %m%n");
    }

    @Test
    public void testLevelDate() {
        checkLine("WARN 2020-01-01 18:11:00.000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n");
        checkLine("WARN 2020-01-01 18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
        checkLine("WARN 2020-01-01_18:11:00,000 mmm", "%level %d{yyyy-MM-dd_HH:mm:ss,SSS} %m%n");
        checkLine("WARN 2020-01-01T18:11:00,000 mmm", "%level %d{yyyy-MM-dd'T'HH:mm:ss,SSS} %m%n");
        checkLine("WARN [2020-01-01T18:11:00,000] mmm", "%level [%d{yyyy-MM-dd'T'HH:mm:ss,SSS}] %m%n");
        checkLine("[WARN] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
        checkLine("[WARN   ] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
        checkLine("[  WARN] 2020-01-01 18:11:00,000 mmm", "[%level] %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");
        checkLine("[WARN] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("[WARN][2020-01-01 18:11:00,000][zzz] mmm", "[%level][%d{yyyy-MM-dd HH:mm:ss,SSS}]%m%n");
        checkLine("[WARN  ] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("[  WARN] [2020-01-01 18:11:00,000] mmm", "[%level] [%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("[  WARN][2020-01-01 18:11:00,000] mmm", "[%level][%d{yyyy-MM-dd HH:mm:ss,SSS}] %m%n");
        checkLine("WARN   2020-01-01 18:11:00,000 mmm", "%level %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n");

        checkLine("WARN 2020-Nov-01 18:11:00,000 mmm", "%level %d{yyyy-MMM-dd HH:mm:ss,SSS} %m%n");

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
    public void nginx() {
        Pair<Boolean, String> pair = LvDefaultFormatDetector.detectFormatOfLine("195.154.122.76 - - [01/Jan/2021:00:21:07 +0300] \"GET /ideaPlugin.html HTTP/1.1\" 200 2366 \"-\" \"Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)\"");
        assert pair != null;
        assert !pair.getFirst();
        assert pair.getSecond().equals("$remote_addr - $remote_user [$time_local] $any");

        pair = LvDefaultFormatDetector.detectFormatOfLine(" 195.154.122.76 - - [01/Jan/2021:00:21:07 +0300] \"GET /ideaPlugin.html HTTP/1.1\" 200 2366 \"-\" \"Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)\"");
        assert pair != null;
        assert pair.getFirst();
        assert pair.getSecond().equals(LvDefaultFormatDetector.UNKNOWN_FORMAT);
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
        checkLine("2020-01-01 18:11:00.0001 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSS} %level %m%n");
        checkLine("2020-01-01 18:11:00.000111 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSSSS} %level %m%n");
        checkLine("2020-01-01 18:11:00.000111222 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSSSSSSS} %level %m%n");
        checkLine("2020-01-01T18:11:00 WARN ddddd", "%d{yyyy-MM-dd'T'HH:mm:ss} %level %m%n");
        checkLine("2020-01-01_18:11:00 WARN ddddd", "%d{yyyy-MM-dd_HH:mm:ss} %level %m%n");

        checkLine("2020-01-01 18:11:00.000+01 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000-06 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000-0600 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000-0630 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000+0630 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000+06:30 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000+1200 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSSz} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 +1200 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 GMT+1200 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 GMT+12:00 WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 GMT WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 UTC WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        // test months
        checkLine("2020-10-01 18:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-11-01 18:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-12-01 18:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-13-01 18:11:00.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        // test hours
        checkLine("2020-01-01 00:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 23:11:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 24:11:00.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 24:11:00.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 30:11:00.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        // test minutes
        checkLine("2020-01-01 18:59:00.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:60:00.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        // test seconds
        checkLine("2020-01-01 18:00:59.000 MSK WARN ddddd", "%d{yyyy-MM-dd HH:mm:ss.SSS z} %level %m%n");
        checkLine("2020-01-01 18:00:60.000 MSK WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        // test timezone
        checkLine("2020-01-01 18:11:00.000!1200 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+3 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+300 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+3000 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+0310 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+0310 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000+03000 WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);
        checkLine("2020-01-01 18:11:00.000WARN ddddd", LvDefaultFormatDetector.UNKNOWN_FORMAT);

        checkLine("20200101 181100 WARN ddddd", "%d{yyyyMMdd HHmmss} %level %m%n");
        checkLine("20200101181100 WARN ddddd", "%d{yyyyMMddHHmmss} %level %m%n");
        checkLine("20200101T181100 WARN ddddd", "%d{yyyyMMdd'T'HHmmss} %level %m%n");
        checkLine("20200101_181100 WARN ddddd", "%d{yyyyMMdd_HHmmss} %level %m%n");
        checkLine("20200101_181100.555 WARN ddddd", "%d{yyyyMMdd_HHmmss.SSS} %level %m%n");
        checkLine("20200101_181100,555 WARN ddddd", "%d{yyyyMMdd_HHmmss,SSS} %level %m%n");
        checkLine("20200101_181100,555+0200 WARN ddddd", "%d{yyyyMMdd_HHmmss,SSSz} %level %m%n");
        checkLine("20200101_181100,555 +02:00 WARN ddddd", "%d{yyyyMMdd_HHmmss,SSS z} %level %m%n");

        checkLine("INFO 20200101_181100,555 ddddd", "%level %d{yyyyMMdd_HHmmss,SSS} %m%n");
        checkLine("INFO 20200101181100555 ddddd", "%level %d{yyyyMMddHHmmssSSS} %m%n");

        checkLine("INFO 26 Sep 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Aug 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Oct 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 26 Nov 2020 10:35:02,625 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO [26 Dec 2020 10:35:02,625] ddddd", "%level [%d{dd MMM yyyy HH:mm:ss,SSS}] %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59,000 ddddd", "%level %d{dd MMM yyyy HH:mm:ss,SSS} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59.000 ddddd", "%level %d{dd MMM yyyy HH:mm:ss.SSS} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59+0300 ddddd", "%level %d{dd MMM yyyy HH:mm:ssz} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59 +03:00 ddddd", "%level %d{dd MMM yyyy HH:mm:ss z} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59.001+03:00 ddddd", "%level %d{dd MMM yyyy HH:mm:ss.SSSz} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59.001 UTC ddddd", "%level %d{dd MMM yyyy HH:mm:ss.SSS z} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59.001 Z ddddd", "%level %d{dd MMM yyyy HH:mm:ss.SSS z} %m%n");
        checkLine("INFO 31 Dec 2020 23:59:59 ddddd", "%level %d{dd MMM yyyy HH:mm:ss} %m%n");
        checkLine("INFO 01 Dec 2018 00:00:00 ddddd", "%level %d{dd MMM yyyy HH:mm:ss} %m%n");
        checkLine("INFO 2018 Dec 01 00:00:00 ddddd", "%level %d{yyyy MMM dd HH:mm:ss} %m%n");
        checkLine("INFO 2018-Dec-01 00:00:00 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss} %m%n");
        checkLine("INFO 2018-Dec-01 00:00:00.000 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss.SSS} %m%n");
        checkLine("INFO 2018-Dec-01 00:00:00,999 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSS} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSS} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999-1030 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSSz} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999 -1030 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSS z} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999 Z ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSS z} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999Z ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSSz} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999999Z ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSSSSSz} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,999999222Z ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSSSSSSSSz} %m%n");
        checkLine("INFO 2018-Oct-31 23:59:59,9999 ddddd", "%level %d{yyyy-MMM-dd HH:mm:ss,SSSS} %m%n");

        checkLine("[18.11.19 18:26:22.160] 00001720 Receive: ParsedData", "[%d{dd.MM.yy HH:mm:ss.SSS}] %m%n");
        checkLine("[18.11.19_18:26:22,160] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[18.11.22_18:26:22,160] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[31.01.22_18:26:22,160] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[01.01.22_00:00:00,000] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[31.12.22_23:59:59,999] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[21.10.02_23:39:39,999] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS}] %m%n");
        checkLine("[21.10.02_23:39:39,999 +0000] 00001720 Receive: ParsedData", "[%d{dd.MM.yy_HH:mm:ss,SSS z}] %m%n");
        checkLine("[21.13.02_23:39:39,999] 00001720 Receive: ParsedData", null);
    }

    private Log4jLogFormat detect(String resourceName) {
        URL url = getClass().getResource(resourceName);
        assert url.getProtocol().equals("file");

        Path path = Paths.get(url.getFile());
        return (Log4jLogFormat) LvDefaultFormatDetector.detectFormat(path);
    }

    private void checkLine(@NonNull String line, @Nullable String format) {
        String detectedFormat = LvDefaultFormatDetector.detectLog4jFormatOfLine(line);
        assertEquals(format, detectedFormat);

        if (format == null || format.equals(LvDefaultFormatDetector.UNKNOWN_FORMAT))
            return;

        Log4jLogFormat f = new Log4jLogFormat(format);
        read(f, line);
    }

}