package com.logviewer.utils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.lang.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegexUtilsTest {

    private void checkMatches(String pattern, String ... samples) throws ParseException {
        Pattern regex = RegexUtils.dateFormatToRegex(pattern);

        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);

        for (String sample : samples) {
            Date date = format.parse(sample);
            String formatted = format.format(date);
            Assert.assertEquals(sample, formatted);

            assert regex.matcher(sample).matches() : sample;
        }
    }

    private void checkNotMatches(String pattern, String ... samples) {
        Pattern regex = RegexUtils.dateFormatToRegex(pattern);

        for (String sample : samples) {
            assert !regex.matcher(sample).matches() : sample;
        }
    }

    @Test
    public void testSimpleDateFormatRegex() throws ParseException {
        checkMatches("y", "1990", "1999", "2000", "2011", "2029");
        checkNotMatches("y", "1", "9999", "19000", "zzz");

        checkMatches("yy", "90", "99", "00", "01", "11", "29");
        checkNotMatches("yy", "1", "2", "zzz", "011");

        checkMatches("yyyy", "1990", "1999", "2000", "2011", "2029");
        checkNotMatches("yyyy", "1", "9999", "19000", "zzz");

        checkMatches("yyyyy", "01990", "01999", "02000", "02011", "02029");
        checkNotMatches("yyyyy", "1999", "2011", "zzz");

        checkMatches("yyyy-M", "2011-1", "2011-11", "2011-12");
        checkNotMatches("yyyy-M", "2011-0", "2011-01", "2011-13", "2011-31");

        checkMatches("yyyy-MM", "2011-01", "2011-11", "2011-12");
        checkNotMatches("yyyy-MM", "2011-00", "2011-13", "2011-31");

        checkMatches("yyyy-MMM", "2011-Jun", "2011-Jul");
        checkNotMatches("yyyy-MMM", "2011-00", "2011-J", "2011-jun");

        checkMatches("yyyy-MMMM", "2011-July");
        checkNotMatches("yyyy-MMMM", "2011-01", "2011-j");

        checkMatches("yyyy-MM-d", "2018-01-1", "2018-01-8", "2018-12-29", "2018-12-31");
        checkNotMatches("yyyy-MM-d", "2018-01-01", "2018-01-41", "2018-01-32", "2018-01-0", "2018-01-aa");

        checkMatches("yyyy-MM-dd", "2018-01-01", "2018-01-08", "2018-12-29", "2018-12-31");
        checkNotMatches("yyyy-MM-dd dd", "2018-01-1", "2018-01-41", "2018-01-32", "2018-01-001", "2018-01-aa");

        checkMatches("yyyy-MM-ddd", "2018-01-001", "2018-12-031");
        checkNotMatches("yyyy-MM-ddd", "2018-01-1", "2018-01-01", "2018-01-041", "2018-01-aa");

        checkMatches("yyyy-MM-dd w", "2018-01-01 1", "2018-01-08 2", "2018-12-29 52");
        checkNotMatches("yyyy-MM-dd w", "2018-01-01 0", "2018-01-01 01", "2018-01-01 111", "2018-01-01 64");

        checkMatches("yyyy-MM-dd ww", "2018-01-01 01", "2018-01-08 02", "2018-12-29 52");
        checkNotMatches("yyyy-MM-dd ww", "2018-01-01 0", "2018-01-01 1", "2018-01-01 111", "2018-01-01 64");

        checkMatches("yyyy-MM-dd www", "2018-01-01 001", "2018-01-08 002", "2018-12-29 052");
        checkNotMatches("yyyy-MM-dd www", "2018-01-01 0", "2018-01-01 1", "2018-01-01 111", "2018-01-01 64");

        checkMatches("yyyy-MM-dd W", "2018-01-01 1", "2018-01-08 2", "2018-01-30 5");
        checkNotMatches("yyyy-MM-dd W", "2018-01-01 0", "2018-01-01 01", "2018-01-01 6");

        checkMatches("yyyy-MM-dd WW", "2018-01-01 01", "2018-01-08 02", "2018-01-30 05");
        checkNotMatches("yyyy-MM-dd WW", "2018-01-01 0", "2018-01-01 1", "2018-01-01 06");

        checkMatches("yyyy-MM-dd E", "2018-01-02 Tue");
        checkMatches("yyyy-MM-dd EEEE", "2018-01-02 Tuesday");

        checkMatches("yyyy-MM-dd H a", "2018-01-02 0 AM", "2018-01-02 1 AM", "2018-01-02 12 PM", "2018-01-02 23 PM");
        checkNotMatches("yyyy-MM-dd H a", "2018-01-02 a AM", "2018-01-02 01 PM", "2018-01-02 24 PM", "2018-01-02 88 PM");
        checkMatches("yyyy-MM-dd HH", "2018-01-02 00", "2018-01-02 03", "2018-01-02 11", "2018-01-02 12", "2018-01-02 23");
        checkNotMatches("yyyy-MM-dd HH", "2018-01-02 a", "2018-01-02 1", "2018-01-02 24", "2018-01-02 88");
        checkMatches("yyyy-MM-dd HHH", "2018-01-02 000", "2018-01-02 003", "2018-01-02 011", "2018-01-02 023");

        checkMatches("yyyy-MM-dd k", "2018-01-02 1", "2018-01-02 11", "2018-01-02 22", "2018-01-02 24");
        checkNotMatches("yyyy-MM-dd k", "2018-01-02 a", "2018-01-02 01", "2018-01-02 0", "2018-01-02 00", "2018-01-02 25", "2018-01-02 024");

        checkMatches("yyyy-MM-dd kk", "2018-01-02 01", "2018-01-02 03", "2018-01-02 11", "2018-01-02 12", "2018-01-02 24");
        checkNotMatches("yyyy-MM-dd kk", "2018-01-02 a", "2018-01-02 1", "2018-01-02 00", "2018-01-02 25", "2018-01-02 88");
        checkMatches("yyyy-MM-dd kkk", "2018-01-02 001", "2018-01-02 003", "2018-01-02 011", "2018-01-02 024");

        checkMatches("yyyy-MM-dd K", "2018-01-02 0", "2018-01-02 1", "2018-01-02 11");
        checkNotMatches("yyyy-MM-dd K", "2018-01-02 a", "2018-01-02 01", "2018-01-02 13");
        checkMatches("yyyy-MM-dd KK", "2018-01-02 00", "2018-01-02 01", "2018-01-02 11");

        checkMatches("yyyy-MM-dd h", "2018-01-02 1", "2018-01-02 9", "2018-01-02 12");
        checkMatches("yyyy-MM-dd hh", "2018-01-02 01", "2018-01-02 09", "2018-01-02 12");
        checkMatches("yyyy-MM-dd hhh", "2018-01-02 001");
        checkNotMatches("yyyy-MM-dd h", "2018-01-02 0", "2018-01-02 01", "2018-01-02 13");

        checkMatches("yyyy-MM-dd HH:m", "2018-01-02 14:0", "2018-01-02 14:9", "2018-01-02 14:10", "2018-01-02 14:59");
        checkNotMatches("yyyy-MM-dd HH:m", "2018-01-02 14:01", "2018-01-02 14:60");
        checkMatches("yyyy-MM-dd HH:mm", "2018-01-02 14:00", "2018-01-02 14:09", "2018-01-02 14:59");

        checkMatches("yyyy-MM-dd HH:mm:s", "2018-01-02 14:11:0", "2018-01-02 14:11:9", "2018-01-02 14:11:10", "2018-01-02 14:11:59");
        checkNotMatches("yyyy-MM-dd HH:mm:s", "2018-01-02 14:11:01", "2018-01-02 14:11:60");
        checkMatches("yyyy-MM-dd HH:mm:ss", "2018-01-02 14:11:00", "2018-01-02 14:11:09", "2018-01-02 14:11:59");

        checkMatches("yyyy-MM-dd S", "2018-01-02 0", "2018-01-02 10", "2018-01-02 110", "2018-01-02 999");
        checkNotMatches("yyyy-MM-dd S", "2018-01-02 a", "2018-01-02 0001", "2018-01-02 1000", "2018-01-02 01");
        checkMatches("yyyy-MM-dd SSSS", "2018-01-02 0000", "2018-01-02 0001", "2018-01-02 0900");
        checkNotMatches("yyyy-MM-dd SSSS", "2018-01-02 00000", "2018-01-02 1000");
        checkMatches("yyyy-MM-dd SS", "2018-01-02 00", "2018-01-02 07", "2018-01-02 110", "2018-01-02 999");
        checkNotMatches("yyyy-MM-dd SS", "2018-01-02 a", "2018-01-02 1", "2018-01-02 1000");

        checkMatches("yyyy-MM-dd zzzz", "2018-01-02 Moscow Standard Time", "2018-01-02 Greenwich Mean Time");
        checkMatches("yyyy-MM-dd zZ", "2018-01-02 MSK+0300", "2018-01-02 UTC+0000");
        checkMatches("yyyy-MM-dd zZZZZZ", "2018-01-02 MSK+0300", "2018-01-02 UTC+0000");

        checkMatches("yyyy-MM-dd zX", "2018-01-02 MSK+03", "2018-01-02 UTCZ");
        checkMatches("yyyy-MM-dd zXX", "2018-01-02 MSK+0300", "2018-01-02 UTCZ");
        checkMatches("yyyy-MM-dd zXXX", "2018-01-02 MSK+03:00", "2018-01-02 UTCZ");
    }

    @Test
    public void testFilePath() {
        doFilePatternTest("*.png", new String[]{
                "a.png", "aaaa.PnG", ".png"
        },
                "a.txt", "a.pnggggg", "a.png/rrr", "zz/a.png", "ssss/ttt.png");

        doFilePatternTest("fff.*", new String[]{
                "fff.txt", "FFF.png", "fff."
        },
                "a.fff", "zz/fff.txt", "fffff.txt");

        doFilePatternTest("rrr/**/l.log", new String[]{
                "rrr/l.log", "rrr/aaa/l.log", "rrr/aaa/bbb/l.log"
        },
                "l.log", "rrr/aaa/llllll.txt");

        doFilePatternTest("**/l.log", new String[]{
                "rrr/l.log", "rrr/aaa/l.log", "l.log", "l.LOG",
        },
                "l.ttt");

        doFilePatternTest("sss/**", new String[]{
                "sss/l.log", "sss/aaa/l.log", "SSS/t.log", "SSS\\t.log"
        },
                "l.ttt", "ssssss", "sss");

        doFilePatternTest("sss/**", new String[]{
                "sss/l.log", "sss/aaa/l.log", "SSS/t.log",
        },
                "l.ttt", "ssssss", "sss");


        doFilePatternTest("sss/**.txt", new String[]{
                "sss/l.txt", "sss/aaa/l.txt"
        },
                "l.txt", "ssssss/t.txt", "sss");
    }

    private void doFilePatternTest(@NonNull String pattern, String[] matches, String ... notMatches) {
        Pattern p1 = RegexUtils.filePattern(pattern);
        Pattern p2 = RegexUtils.filePattern(pattern.replace('/', '\\'));

        for (String s : matches) {
            String s2 = s.replace('/', '\\');

            assertTrue(pattern + " !~= " + s, p1.matcher(s).matches());
            assertTrue(pattern + " !~= " + s, p2.matcher(s).matches());
            assertTrue(pattern + " !~= " + s2, p1.matcher(s2).matches());
            assertTrue(pattern + " !~= " + s2, p2.matcher(s2).matches());
        }

        for (String s : notMatches) {
            String s2 = s.replace('/', '\\');

            assertFalse(pattern + " ~= " + s, p1.matcher(s).matches());
            assertFalse(pattern + " ~= " + s, p2.matcher(s).matches());
            assertFalse(pattern + " ~= " + s, p1.matcher(s2).matches());
            assertFalse(pattern + " ~= " + s, p2.matcher(s2).matches());
        }
    }
}
