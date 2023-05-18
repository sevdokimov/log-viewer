package com.logviewer.data2;

import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Locale;

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

    @Nullable
    Locale getLocale();

    /**
     * @return {@code true} if log entry contains a date.
     * @see com.logviewer.utils.LvDateUtils#isDateFormatFull(DateFormat)
     */
    boolean hasFullDate();

    void validate() throws IllegalArgumentException;

    String getHumanReadableString();

    default void loadGlobalConfig(@NonNull Environment env) {

    }

    interface FieldDescriptor {
        FieldDescriptor[] EMPTY_ARRAY = new FieldDescriptor[0];

        @NonNull
        String name();
        @Nullable
        String type();
    }
}
