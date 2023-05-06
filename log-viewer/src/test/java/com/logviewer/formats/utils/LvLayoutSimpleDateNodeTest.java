package com.logviewer.formats.utils;

import com.logviewer.TestUtils;
import com.logviewer.utils.LvDateUtils;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.logviewer.formats.utils.LvLayoutNode.PARSE_FAILED;
import static org.junit.Assert.assertEquals;

public class LvLayoutSimpleDateNodeTest {

    @Test
    public void testInvalidPattern() {
        TestUtils.assertError(IllegalArgumentException.class, () -> new LvLayoutSimpleDateNode("yyyy'"));
    }

    @Test
    public void testBounds() {
        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode("yyyy-MM-dd HH:mm1");

        String s = "1232021-11-01 20:5517";

        int endIdx = node.parse(s, 3, s.length() - 1);
        assertEquals(s.length() - 1, endIdx);

        endIdx = node.parse(s, 3, s.length());
        assertEquals(s.length() - 1, endIdx);

        endIdx = node.parse(s, 2, s.length());
        assertEquals(PARSE_FAILED, endIdx);

        endIdx = node.parse(s, 3, s.length() - 5);
        assertEquals(PARSE_FAILED, endIdx);

        endIdx = node.parse(s.substring(0, 14), 3, 3 + 14);
        assertEquals(PARSE_FAILED, endIdx);
    }

    @Test
    public void testworkaroundJDK8031085withInvalidPattern() {
        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode("yyyyMMddHHmmssSSS");
        int res = node.parse("fsdfsdfsd", 0, 6);
        assertEquals(PARSE_FAILED, res);
    }

    @Test
    public void testStringEnd() {
        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode("yyyyMMddHHmmss");
        int res = node.parse("20210101220000", 0, 6);
        assertEquals(PARSE_FAILED, res);

        res = node.parse("20210101220000", 0, 14);
        assertEquals(14, res);
        assert node.getCurrentDate() > 0;
    }

    @Test
    public void formatEqualsToSimpleDateFormat() throws Exception {
        checkPattern("yyyy-MM-dd HH:mm:ss.SSS", "2021-01-13 20:01:22.333");
        checkPattern("yyyy-MM-dd HH:mm:ss", "2021-01-13 20:01:22");
        checkPattern("yyyy-MM-dd HH:mm", "2021-01-13 20:01");
        checkPattern("yyyy-MM-dd'T'HH:mm", "2021-01-13T20:01");
        checkPattern("yyyy-MM-dd:HH:mm", "2021-01-13:20:01");
        checkPattern("yyyy-MM-dd HH:mm G", "2021-01-13 20:01 AD");

        checkPattern("yy-MM-dd HH:mm", "21-01-13 20:01");
        checkPattern("MM/dd/yy HH:mm", "01/13/21 20:01");
        checkPattern("MM/dd/y HH:mm", "01/13/2021 20:01");

        checkPattern("yyyy-M-dd HH:mm", "2021-1-01 02:02");
        checkPattern("yyyy-MMM-dd HH:mm", "2021-Jan-01 02:02");

        checkPattern("yyyy-MM-d HH:mm", "2021-01-1 02:02");

        checkPattern("yyyy-MM-dd H:m", "2021-01-01 2:2");

        checkPattern("yyyy-MM-dd F HH:mm", "2021-01-01 1 02:02");
        checkPattern("yyyy-MM-dd E HH:mm", "2021-01-01 Fri 02:02");
        checkPattern("yyyy-MM-dd EE HH:mm", "2021-01-01 Fri 02:02");
        checkPattern("yyyy-MM-dd EEE HH:mm", "2021-01-01 Fri 02:02");

        checkPattern("yyyyMMddHHmmss", "20210101020255");
        checkPattern("yyyyMMddHHmmssSSS", "20210101020255111");

        checkPattern("yyyy-MM-dd HH:mm a", "2021-01-01 02:02 AM");

        checkPattern("yyyy-MM-dd HH", "2020-01-01 20");

        TestUtils.withTimeZone("UTC", () -> {
            checkPattern("yyyy-MM-dd HH:mm z", "2021-01-01 02:02 UTC");
            checkPattern("yyyy-MM-dd HH:mm zz", "2021-01-01 02:02 UTC");
            checkPattern("yyyy-MM-dd HH:mm zzz", "2021-01-01 02:02 UTC");

            checkPattern("yyyyMMddHHmmssSSSz", "20210101020255111UTC");
            checkPattern("yyyyMMddHHmmssSSSzz", "20210101020255111UTC");
            checkPattern("yyyyMMddHHmmssSSSzzz", "20210101020255111UTC");

            checkPattern("yyyy-MM-dd HH:mm Z", "2021-01-01 02:02 +0000");
            checkPattern("yyyy-MM-dd HH:mm ZZ", "2021-01-01 02:02 +0000");
            checkPattern("yyyy-MM-dd HH:mm ZZZ", "2021-01-01 02:02 +0000");
            checkPattern("yyyy-MM-dd HH:mm ZZZZ", "2021-01-01 02:02 +0000", false);
            checkPattern("yyyy-MM-dd HH:mm ZZZZZ", "2021-01-01 02:02 +0000");
            checkPattern("yyyyMMddHHmmssSSSZ", "20210101020255111+0000");

            checkPattern("yyyy-MM-dd HH:mm X", "2021-01-01 02:02 Z");
            checkPattern("yyyy-MM-dd HH:mm X", "2021-01-01 02:02 +0000", false);
            checkPattern("yyyy-MM-dd HH:mm XX", "2021-01-01 02:02 +0000", false);
            checkPattern("yyyy-MM-dd HH:mm XX", "2021-01-01 02:02 Z");
            checkPattern("yyyy-MM-dd HH:mm XXX", "2021-01-01 02:02 Z", true);
            checkPattern("yyyy-MM-dd HH:mm XXX", "2021-01-01 02:02 +00:00", false);
        });

        TestUtils.withTimeZone("GMT+0300", () -> {
            checkPattern("yyyy-MM-dd HH:mm z", "2021-01-01 02:02 MSK");
            checkPattern("yyyy-MM-dd HH:mm zz", "2021-01-01 02:02 MSK");
            checkPattern("yyyy-MM-dd HH:mm zzz", "2021-01-01 02:02 MSK");

            checkPattern("yyyy-MM-dd HH:mm Z", "2021-01-01 02:02 +0300");
            checkPattern("yyyy-MM-dd HH:mm ZZ", "2021-01-01 02:02 +0300");
            checkPattern("yyyy-MM-dd HH:mm ZZZ", "2021-01-01 02:02 +0300");

            checkPattern("yyyy-MM-dd HH:mm X", "2021-01-01 02:02 +03");
            checkPattern("yyyy-MM-dd HH:mm XX", "2021-01-01 02:02 +0300");
            checkPattern("yyyy-MM-dd HH:mm XXX", "2021-01-01 02:02 +03:00");
        });
    }

    private void checkPattern(String pattern, String s) throws ParseException {
        checkPattern(pattern, s, true);
    }

    private void checkPattern(String pattern, String s, boolean mustBeEqual) throws ParseException {
        SimpleDateFormat simpleFormat = new SimpleDateFormat(pattern);

        if (mustBeEqual)
            assertEquals(s, simpleFormat.format(simpleFormat.parse(s)));

        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode(pattern);

        assert node.isFull();

        int endIdx = node.parse(s, 0, s.length());
        assertEquals(s.length(), endIdx);

        long date = node.getCurrentDate();
        assert date > 0;

        if (mustBeEqual) {
            String formatted = simpleFormat.format(new Date(TimeUnit.NANOSECONDS.toMillis(date)));
            assertEquals(formatted, s);
        } else {
            String formatted = simpleFormat.format(new Date(TimeUnit.NANOSECONDS.toMillis(date)));

            endIdx = node.parse(formatted, 0, formatted.length());
            assertEquals(formatted.length(), endIdx);

            assertEquals(date, node.getCurrentDate());
        }
    }

    @Test
    public void testMilliseconds() {
        checkMilliseconds("y/MM/dd HHmmss.SSS", "2020/01/01 020202.111");
        checkMilliseconds("y/MM/dd HHmmss.SSSS", "2020/01/01 020202.1111");
        checkMilliseconds("y/MM/dd HHmmss.SSSSS", "2020/01/01 020202.11122");
        checkMilliseconds("y/MM/dd HHmmss.SSSSSS", "2020/01/01 020202.111222");
    }

    @Test
    public void testSetZone() {
        LvLayoutDateNode node = new LvLayoutSimpleDateNode("y/MM/dd HHmmss").withTimeZone(TimeZone.getTimeZone("GMT+1000"));
        node = node.withTimeZone(TimeZone.getTimeZone("GMT+0300"));

        String s = "2020/01/01 120000";
        int parse = node.parse(s, 0, s.length());
        assertEquals(s.length(), parse);

        assertEquals(LvDateUtils.toNanos(OffsetDateTime.parse("2020-01-01T12:00:00+03:00").toInstant()), node.getCurrentDate());
    }

    private void checkMilliseconds(String pattern, String s) {
        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode(pattern);

        assert node.isFull();

        int endIdx = node.parse(s, 0, s.length());
        assertEquals(s.length(), endIdx);

        long date = node.getCurrentDate();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());

        Instant instant = Instant.from(formatter.parse(s));
        assertEquals(date / 1000_000_000, instant.getEpochSecond());
        assertEquals(date % 1000_000_000, instant.getNano());
    }

    @Test
    public void nonFullPatterns() {
        checkNonFull("MM-dd HH:mm:ss.SSS", "01-13 20:01:22.333");
        checkNonFull("MMM-dd HH:mm:ss.SSS", "Jan-13 20:01:22.333");
        checkNonFull("HH:mm:ss.SSS", "20:01:22.333");
        checkNonFull("HH:mm:ss", "20:01:22");
        checkNonFull("HH:mm", "20:01");
        checkNonFull("yyyy HH:mm", "2020 20:01");
        checkNonFull("yyyy-MM HH:mm", "2020-01 20:01");
        checkNonFull("yyyy-dd HH:mm", "2020-01 20:01");

        checkNonFull("yyyy-MM-dd", "2020-01-01");
        checkNonFull("yyyy-MM-dd mm", "2020-01-01 01");
        checkNonFull("yyyy-MM-dd mm:ss", "2020-01-01 02:02");
    }

    private void checkNonFull(String pattern, String s) {
        LvLayoutSimpleDateNode node = new LvLayoutSimpleDateNode(pattern);
        assert !node.isFull();

        int endIdx = node.parse(s, 0, s.length());
        assertEquals(s.length(), endIdx);
    }
}