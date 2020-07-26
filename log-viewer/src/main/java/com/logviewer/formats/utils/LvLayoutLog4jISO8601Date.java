package com.logviewer.formats.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date in ISO8601 format. log4J_1 and log2J_2  format ISO8601 in different way
 * Log4j_1 :  %d{ISO8601} = 2011-10-13 18:33:45,000
 * Log4j_2 :  %d{ISO8601} = 2011-10-13T18:33:45,000
 *
 * This class support both of those formats.
 *
 * LvLayoutLog4jISO8601Date works much faster than {@link java.text.SimpleDateFormat}
 */
public class LvLayoutLog4jISO8601Date extends LvLayoutDateNode {

    private static final Pattern SUPPORTED_PATTERN = Pattern.compile("yyyy-MM-dd(?:'T'|[_T ])HH:mm:ss([,.]SSS)?");

    private final boolean hasMilliseconds;

    private transient Calendar calendar;

    public LvLayoutLog4jISO8601Date(boolean hasMilliseconds) {
        this.hasMilliseconds = hasMilliseconds;
    }

    @Override
    public int parse(String s, int offset, int end) {
        int expectedLength = hasMilliseconds ? 23 : 19;

        if (end - offset < expectedLength) {
            return PARSE_FAILED;
        }

        int year = readInt(s, offset, offset + 4);
        if (year < 1970 || year > 2034)
            return PARSE_FAILED;

        offset += 4;

        if (s.charAt(offset++) != '-')
            return PARSE_FAILED;

        int mm = readInt(s, offset, offset + 2);
        if (mm < 1 || mm > 12)
            return PARSE_FAILED;

        offset += 2;

        if (s.charAt(offset++) != '-')
            return PARSE_FAILED;

        int dd = readInt(s, offset, offset + 2);
        if (dd < 1 || dd > 31)
            return PARSE_FAILED;

        offset += 2;

        char a = s.charAt(offset++);
        if (a != 'T' && a != ' ' && a != '_')
            return PARSE_FAILED;

        int hh = readInt(s, offset, offset + 2);
        if (hh < 0 || hh > 23)
            return PARSE_FAILED;

        offset += 2;

        if (s.charAt(offset++) != ':')
            return PARSE_FAILED;

        int min = readInt(s, offset, offset + 2);
        if (min < 0 || min > 59)
            return PARSE_FAILED;

        offset += 2;

        if (s.charAt(offset++) != ':')
            return PARSE_FAILED;

        int sec = readInt(s, offset, offset + 2);
        if (sec < 0 || sec > 59)
            return PARSE_FAILED;

        offset += 2;

        int sss;
        if (hasMilliseconds) {
            a = s.charAt(offset++);
            if (a != ',' && a != '.')
                return PARSE_FAILED;

            sss = readInt(s, offset, offset + 3);
            if (sss < 0 || sss > 999)
                return PARSE_FAILED;

            offset += 3;
        } else {
            sss = 0;
        }

        if (calendar == null)
            calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, mm - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dd);
        calendar.set(Calendar.HOUR_OF_DAY, hh);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, sss);

        this.currentDate = calendar.getTimeInMillis();

        return offset;
    }

    private static int readInt(String s, int offset, int end) {
        int res = 0;

        while (offset < end) {
            char a = s.charAt(offset++);

            if (a < '0' || a > '9')
                return -1;

            res = res * 10 + (a - '0');
        }

        return res;
    }

    @Override
    public boolean isFull() {
        return true;
    }

    @Nullable
    public static LvLayoutLog4jISO8601Date fromPattern(@Nonnull String pattern) {
        Matcher matcher = LvLayoutLog4jISO8601Date.SUPPORTED_PATTERN.matcher(pattern);
        if (matcher.matches())
            return new LvLayoutLog4jISO8601Date(matcher.group(1) != null);

        return null;
    }

    @Override
    public LvLayoutNode clone() {
        return new LvLayoutLog4jISO8601Date(hasMilliseconds);
    }
}
