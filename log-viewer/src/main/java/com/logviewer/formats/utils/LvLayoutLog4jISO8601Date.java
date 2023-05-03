package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
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

    private static final Pattern SUPPORTED_PATTERN = Pattern.compile("yyyy([-/])MM\\1dd(?:'T'|[_T ])HH:mm:ss(?<milliseconds>[,.]SSS(?:SSSSSS|SSS|S)?)?(?<timezone>z+|Z+|X+)?");

    private static final int[] MILLI_TO_NANO = {1000_000, 100_000, 10_000, 1000, 100, 10, 1};

    private final int milliseconds;

    private final boolean hasTimezone;

    private transient Calendar calendar;

    private transient String currentTimezoneStr;

    public LvLayoutLog4jISO8601Date(boolean hasMilliseconds) {
        this(hasMilliseconds ? 3 : 0, false, null);
    }

    public LvLayoutLog4jISO8601Date(int milliseconds, boolean hasTimezone) {
        this(milliseconds, hasTimezone, null);
    }

    public LvLayoutLog4jISO8601Date(int milliseconds, boolean hasTimezone, TimeZone zone) {
        this(milliseconds, hasTimezone, null, zone);
    }
    public LvLayoutLog4jISO8601Date(int milliseconds, boolean hasTimezone, Locale locale, TimeZone zone) {
        super(locale, zone);
        this.milliseconds = milliseconds;
        this.hasTimezone = hasTimezone;
    }

    @Override
    public int parse(String s, int offset, int end) {
        int expectedLength = 19 + (milliseconds == 0 ? 0 : milliseconds + 1);

        if (end - offset < expectedLength) {
            return PARSE_FAILED;
        }

        int year = readInt(s, offset, offset + 4);
        if (year < 1970 || year > 2034)
            return PARSE_FAILED;

        offset += 4;

        char dateSeparator = s.charAt(offset++);
        if (dateSeparator != '-' && dateSeparator != '/')
            return PARSE_FAILED;

        int mm = readInt(s, offset, offset + 2);
        if (mm < 1 || mm > 12)
            return PARSE_FAILED;

        offset += 2;

        if (s.charAt(offset++) != dateSeparator)
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

        int nano;
        if (milliseconds > 0) {
            a = s.charAt(offset++);
            if (a != ',' && a != '.')
                return PARSE_FAILED;

            nano = readInt(s, offset, offset + milliseconds);
            if (nano < 0)
                return PARSE_FAILED;

            nano *= MILLI_TO_NANO[milliseconds - 3];

            offset += milliseconds;
        } else {
            nano = 0;
        }

        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.MILLISECOND, 0);

            if (zone != null)
                calendar.setTimeZone(zone);
        }

        if (hasTimezone) {
            offset = parseAndSetTimezone(s, offset, calendar);
            if (offset < 0)
                return PARSE_FAILED;
        }

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, mm - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dd);
        calendar.set(Calendar.HOUR_OF_DAY, hh);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);

        this.currentDate = LvDateUtils.toNanos(calendar.getTimeInMillis()) + nano;

        return offset;
    }

    private int parseAndSetTimezone(String s, int offset, Calendar calendar) {
        if (currentTimezoneStr != null && s.startsWith(currentTimezoneStr, offset)) {
            return offset + currentTimezoneStr.length();
        }

        if (offset >= s.length())
            return -1;

        ParsePosition position = new ParsePosition(offset);
        TimeZone res = FastDateTimeParser.parseTimezone(s, position);
        if (res == null)
            return -1;

        calendar.setTimeZone(res);
        currentTimezoneStr = s.substring(offset, position.getIndex());
        return position.getIndex();
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
    public static LvLayoutLog4jISO8601Date fromPattern(@NonNull String pattern) {
        Matcher matcher = LvLayoutLog4jISO8601Date.SUPPORTED_PATTERN.matcher(pattern);
        if (!matcher.matches())
            return null;

        String ms = matcher.group("milliseconds");
        return new LvLayoutLog4jISO8601Date(ms == null ? 0 : ms.length() - 1, matcher.group("timezone") != null);
    }

    @Override
    public LvLayoutDateNode clone() {
        return new LvLayoutLog4jISO8601Date(milliseconds, hasTimezone, locale, zone);
    }
}
