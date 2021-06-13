package com.logviewer.formats;

import com.logviewer.formats.utils.LvLayoutDateNode;
import com.logviewer.formats.utils.LvLayoutLog4jISO8601Date;
import com.logviewer.formats.utils.LvLayoutNode;
import com.logviewer.utils.LvDateUtils;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LvLayoutLog4jISO8601DateTest {

    @Test
    public void minValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "2000-01-01 00:00:00,000";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), LvDateUtils.toNanos(format.parse(date)));
    }

    @Test
    public void maxValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "2030-12-31 23:59:59,999";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), LvDateUtils.toNanos(format.parse(date)));
    }

    @Test
    public void parseMilliseconds() {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(6, true);
        String date = "2030-12-31 23:59:59,999111Z";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        assertEquals(999111000, field.getCurrentDate() % 1_000_000_000);
    }

    @Test
    public void parseNanoseconds() {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(9, true);
        String date = "2030-12-31 23:59:59,999111456Z";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        assertEquals(999111456, field.getCurrentDate() % 1_000_000_000);
    }

    @Test
    public void timzZoneHH() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSX");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0300"));

        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(3, true);
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!03", 0, "2000-12-31 23:59:59,999!03".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!0300", 0, "2000-12-31 23:59:59,999!0300".length()));

        String s = "2000-12-31 23:59:59,999+03";
        int parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(LvDateUtils.toNanos(df.parse(s)), field.getCurrentDate());

        df.setTimeZone(TimeZone.getTimeZone("GMT-0800"));
        s = "2000-12-31 23:59:59,999-08";
        parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(LvDateUtils.toNanos(df.parse(s)), field.getCurrentDate());
    }

    @Test
    public void timzZoneZ() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSXXX");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(3, true);
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999", 0, "2000-12-31 23:59:59,999".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999k", 0, "2000-12-31 23:59:59,999k".length()));

        String s = "2000-12-31 23:59:59,999Z";
        int parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(LvDateUtils.toNanos(df.parse(s)), field.getCurrentDate());
    }

    @Test
    public void timzeZoneHHMM() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSXX");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0300"));

        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(3, true);
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999+030", 0, "2000-12-31 23:59:59,999+030".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!0300", 0, "2000-12-31 23:59:59,999!0300".length()));

        String s = "2000-12-31 23:59:59,999+0300";
        int parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(LvDateUtils.toNanos(df.parse(s)), field.getCurrentDate());

        df.setTimeZone(TimeZone.getTimeZone("GMT-0800"));
        s = "2000-12-31 23:59:59,999-0800";
        parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(LvDateUtils.toNanos(df.parse(s)), field.getCurrentDate());
    }

    @Test
    public void valueExceedLimit() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);

        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("3000-12-31 23:59:59,999", 0, s.length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("1000-12-31 23:59:59,999", 0, s.length()));

        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-13-31 23:59:59,999", 0, s.length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-00-31 23:59:59,999", 0, s.length()));

        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-00 23:59:59,999", 0, s.length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-32 23:59:59,999", 0, s.length()));

        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 24:59:59,999", 0, s.length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:60:59,999", 0, s.length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:60,999", 0, s.length()));
    }

    @Test
    public void differentSeparator() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);

        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999", 0, s.length()));
        assertEquals(s.length(), field.parse("2000-12-31 23:59:59,999", 0, s.length()));
        assertEquals(s.length(), field.parse("2000-12-31_23:59:59,999", 0, s.length()));
        assertEquals(s.length(), field.parse("2000-12-31_23:59:59.999", 0, s.length()));
        assertEquals(s.length(), field.parse("2000/12/31_23:59:59.999", 0, s.length()));
    }

    @Test
    public void withTimezone() throws ParseException {
        checkLayoutWithTimeZone(new LvLayoutLog4jISO8601Date(true));
    }

    public static void checkLayoutWithTimeZone(LvLayoutDateNode field) throws ParseException {
        String s = "2030-12-31 23:59:59,999";

        field = field.withTimeZone(TimeZone.getTimeZone("EST"));

        if (field.parse(s, 0, s.length()) != s.length())
            throw new RuntimeException();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

        format.setTimeZone(field.getZone());
        assertEquals(LvDateUtils.toNanos(format.parse(s)), field.getCurrentDate());

        field.withTimeZone(TimeZone.getTimeZone("GMT"));

        if (field.parse(s, 0, s.length()) != s.length())
            throw new RuntimeException();

        format.setTimeZone(field.getZone());
        assertEquals(LvDateUtils.toNanos(format.parse(s)), field.getCurrentDate());
    }

    @Test
    public void fromPattern() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss.SSS");
        assert field != null;

        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999", 0, s.length()));

        s = "2030-12-31T23:59:59,999111Z";
        field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss.SSSSSSz");
        assert field != null;
        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999222Z", 0, s.length()));

        s = "2030-12-31T23:59:59,9991";
        field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss.SSSS");
        assert field != null;
        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999222Z", 0, s.length()));

        s = "2030-12-31 23:59:59";
        field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss");
        assert field != null;
        assertEquals(s.length(), field.parse("2000-12-31T23:59:59", 0, s.length()));

        s = "2030/12/31_23:59:59";
        field = LvLayoutLog4jISO8601Date.fromPattern("yyyy/MM/dd_HH:mm:ss");
        assert field != null;

        assertEquals(s.length(), field.parse("2000/12/31_23:59:59", 0, s.length()));

        assertNull(LvLayoutLog4jISO8601Date.fromPattern("yyyy/MM-dd_HH:mm:ss"));
    }

    @Test
    public void offset() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "___2000-01-01 00:00:00,000_";
        assertEquals(date.length() - 1, field.parse(date, 3, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), LvDateUtils.toNanos(format.parse(date.substring(3))));
    }

}
