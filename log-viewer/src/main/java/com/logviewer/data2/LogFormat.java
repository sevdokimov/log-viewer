package com.logviewer.data2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.text.DateFormat;

/**
 *
 */
public interface LogFormat {

    LogReader createReader();

    FieldDescriptor[] getFields();

    default int getFieldIndexByName(@Nonnull String fieldName) {
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

        @Nonnull
        String name();
        @Nullable
        String type();
    }
}
