package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.text.DateFormat;

/**
 *
 */
public interface LogFormat {

    LogReader createReader();

    FieldDescriptor[] getFields();

    default int getFieldIndexByName(@NonNull String fieldName) {
        FieldDescriptor[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].name().equals(fieldName))
                return i;
        }

        return -1;
    }

    @Nullable
    Charset getCharset();

    /**
     * @return {@code true} if log entry contains a date.
     * @see com.logviewer.utils.LvDateUtils#isDateFormatFull(DateFormat)
     */
    boolean hasFullDate();

    interface FieldDescriptor {
        FieldDescriptor[] EMPTY_ARRAY = new FieldDescriptor[0];

        @NonNull
        String name();
        @Nullable
        String type();
    }
}
