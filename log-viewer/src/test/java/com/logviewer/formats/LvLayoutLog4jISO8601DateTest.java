package com.logviewer.formats;

import com.logviewer.formats.utils.LvLayoutLog4jISO8601Date;
import com.logviewer.formats.utils.LvLayoutNode;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;

public class LvLayoutLog4jISO8601DateTest {

    @Test
    public void minValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date();
        String date = "2000-01-01 00:00:00,000";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date).getTime());
    }

    @Test
    public void maxValue() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date();
        String date = "2030-12-31 23:59:59,999";
        assertEquals(date.length(), field.parse(date, 0, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date).getTime());
    }

    @Test
    public void valueExceedLimit() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date();

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
    public void separatorT() {
        String s = "2030-12-31 23:59:59,999";
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date();

        assertEquals(s.length(), field.parse("2000-12-31T23:59:59,999", 0, s.length()));
    }

    @Test
    public void offset() throws ParseException {
        LvLayoutLog4jISO8601Date field = new LvLayoutLog4jISO8601Date();
        String date = "___2000-01-01 00:00:00,000_";
        assertEquals(date.length() - 1, field.parse(date, 3, date.length()));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        assertEquals(field.getCurrentDate(), format.parse(date.substring(3)).getTime());
    }

}
