package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;

import java.util.regex.Pattern;

/**
 *
 */
public class ExceptionOnlyPredicate implements RecordPredicate {

    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("" +
            "^\\tat " +
            "(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" + // package name
            "\\." +
            "(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*|<\\w+>)" + // method name
            "\\(.+\\)" // file name
            , Pattern.MULTILINE);

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        if (!record.getMessage().contains("\tat "))
            return false;

        return EXCEPTION_PATTERN.matcher(record.getMessage()).find();
    }
}
