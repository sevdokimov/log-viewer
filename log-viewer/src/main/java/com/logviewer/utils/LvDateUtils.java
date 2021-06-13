package com.logviewer.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class LvDateUtils {

    public static final int[] DATE_FIELDS = new int[]{Calendar.MILLISECOND,
            Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY,
            Calendar.DAY_OF_YEAR, Calendar.MONTH, Calendar.YEAR};

    private LvDateUtils() {
    }

    /**
     * @return {@code true} if the date format can be used to sort log entries. For example "HH:mm:ss" format cannot
     * be used for sorting because it doesn't contain date.
     */
    public static boolean isDateFormatFull(@NonNull DateFormat dateFormat) {
        Calendar c = Calendar.getInstance(dateFormat.getTimeZone());
        c.set(2020, Calendar.FEBRUARY, 25, 1, 1, 4);
        c.setTimeInMillis(333);

        int firstExistField = 0;

        while (firstExistField < DATE_FIELDS.length && !isFieldPresent(dateFormat, c, DATE_FIELDS[firstExistField], 1)) {
            firstExistField++;
        }

        if (firstExistField > 3) // 3 - hours, minimum valid date is "yyyy-MM-dd HH"
            return false;

        for (int i = firstExistField; i < DATE_FIELDS.length; i++) {
            if (!isFieldPresent(dateFormat, c, DATE_FIELDS[i], 1))
                return false;
        }

        if (isFieldPresent(dateFormat, c, Calendar.HOUR, 1) && !isFieldPresent(dateFormat, c, Calendar.HOUR, 12)) { // check what date format distinguish "11:00 AM" and "11:00 PM"
            return false;
        }

        return true;
    }

    private static boolean isFieldPresent(@NonNull DateFormat dateFormat, Calendar c, int field, int incrementCount) {
        try {
            Date initDate = dateFormat.parse(dateFormat.format(c.getTime()));

            c.add(field, incrementCount);

            Date newDate = dateFormat.parse(dateFormat.format(c.getTime()));

            c.add(field, -incrementCount);

            return initDate.before(newDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static long toNanos(@NonNull Date date) {
        return toNanos(date.getTime());
    }

    public static long toNanos(@NonNull Instant instant) {
        return instant.getEpochSecond() * 1000_000_000L + instant.getNano();
    }

    public static long toNanos(long millisecond) {
        if (millisecond <= 0)
            return millisecond;

        if (millisecond > Utils.MAX_TIME_MILLIS)
            throw new IllegalArgumentException("Not a milliseconds: " + millisecond);

        return millisecond * 1_000_000;
    }

    public static Instant toInstant(TemporalAccessor accessor, @Nullable TimeZone zone) {
        if (!accessor.isSupported(ChronoField.INSTANT_SECONDS)) {
            LocalDateTime localDateTime = LocalDateTime.from(accessor);
            return localDateTime.atZone(zone == null ? ZoneId.systemDefault() : zone.toZoneId()).toInstant();
        } else {
            return Instant.from(accessor);
        }
    }
}
