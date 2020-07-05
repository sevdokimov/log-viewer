package com.logviewer.formats.utils;

import java.util.Calendar;

/**
 * Date in ISO8601 format. log4J_1 and log2J_2  format ISO8601 in different way
 * Log4j_1 :  %d{ISO8601} = 2011-10-13 18:33:45,000
 * Log4j_2 :  %d{ISO8601} = 2011-10-13T18:33:45,000
 *
 * This class support both of those formats.
 */
public class LvLayoutLog4jISO8601Date extends LvLayoutDateNode {

    private Calendar calendar;

    @Override
    public int parse(String s, int offset, int end) {
        if (end - offset < "2011-10-13T18:33:45,000".length()) {
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
        if (a != 'T' && a != ' ')
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

        if (s.charAt(offset++) != ',')
            return PARSE_FAILED;

        int sss = readInt(s, offset, offset + 3);
        if (sss < 0 || sss > 999)
            return PARSE_FAILED;

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

        return offset + 3;
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

    @Override
    public LvLayoutNode clone() {
        return new LvLayoutLog4jISO8601Date();
    }
}
