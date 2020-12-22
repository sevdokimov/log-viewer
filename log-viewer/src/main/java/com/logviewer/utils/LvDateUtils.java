package com.logviewer.utils;

import org.springframework.lang.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

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

        if (firstExistField == DATE_FIELDS.length)
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



}
