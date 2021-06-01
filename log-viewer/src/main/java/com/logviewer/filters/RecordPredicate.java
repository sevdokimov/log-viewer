package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;

/**
 *
 */
public interface RecordPredicate {

    boolean test(LogRecord record, LogFilterContext ctx);

    default RecordPredicate not() {
        return new NotPredicate(this);
    }
}
