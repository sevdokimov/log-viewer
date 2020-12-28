package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class FieldTypes {

    public static final String DATE = "date";

    public static final String LEVEL = "level";
    public static final String LEVEL_LOGBACK = "level/logback";
    public static final String LEVEL_LOG4J = "level/log4j";

    public static final String JAVA_CLASS = "class";

    public static final String MESSAGE = "message";
    public static final String THREAD = "thread";

    public static final String NDC = "ndc";
    
    public static final String MDC = "mdc";

    /**
     * the number of milliseconds elapsed since the start of the application until the creation of the logging event.
     */
    public static final String RELATIVE_TIMESTAMP = "relativeTimestamp";

    public static boolean is(@Nullable String fieldType, @NonNull String expectedType) {
        if (fieldType == null || !fieldType.startsWith(expectedType))
            return false;

        return fieldType.length() == expectedType.length() || fieldType.startsWith("/", expectedType.length());
    }
}
