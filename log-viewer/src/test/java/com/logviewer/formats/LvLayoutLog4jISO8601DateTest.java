package com.logviewer.formats;

import com.logviewer.formats.utils.LvLayoutLog4jISO8601Date;
import com.logviewer.formats.utils.LvLayoutNode;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class LvLayoutLog4jISO8601DateTest {

    @Test
    public void minValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "2000-01-01 00:00:00,000";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date).getTime());
    }

    @Test
    public void maxValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "2030-12-31 23:59:59,999";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date).getTime());
    }

    @Test
    public void timzeZoneHH() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSX");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0300"));

        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true, 3);
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!03", 0, "2000-12-31 23:59:59,999!03".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!0300", 0, "2000-12-31 23:59:59,999!0300".length()));

        String s = "2000-12-31 23:59:59,999+03";
        int parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(df.parse(s).getTime(), field.getCurrentDate());

        df.setTimeZone(TimeZone.getTimeZone("GMT-0800"));
        s = "2000-12-31 23:59:59,999-08";
        parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(df.parse(s).getTime(), field.getCurrentDate());
    }

    @Test
    public void timzeZoneHHMM() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSXX");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0300"));

        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true, 5);
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999+030", 0, "2000-12-31 23:59:59,999+030".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999+03", 0, "2000-12-31 23:59:59,999+03".length()));
        assertEquals(LvLayoutNode.PARSE_FAILED, field.parse("2000-12-31 23:59:59,999!0300", 0, "2000-12-31 23:59:59,999!0300".length()));

        String s = "2000-12-31 23:59:59,999+0300";
        int parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(df.parse(s).getTime(), field.getCurrentDate());

        df.setTimeZone(TimeZone.getTimeZone("GMT-0800"));
        s = "2000-12-31 23:59:59,999-0800";
        parse = field.parse(s, 0, s.length());
        assertEquals(s.length(), parse);
        assertEquals(df.parse(s).getTime(), field.getCurrentDate());
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
    }

    @Test
    public void fromPattern() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss.SSS");
        assert field != null;

        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999", 0, s.length()));

        s = "2030-12-31 23:59:59";
        field = LvLayoutLog4jISO8601Date.fromPattern("yyyy-MM-dd HH:mm:ss");
        assert field != null;

        assertEquals(s.length(), field.parse("2000-12-31T23:59:59", 0, s.length()));
    }

    @Test
    public void offset() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date(true);
        String date = "___2000-01-01 00:00:00,000_";
        assertEquals(date.length() - 1, field.parse(date, 3, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date.substring(3)).getTime());
    }

}
