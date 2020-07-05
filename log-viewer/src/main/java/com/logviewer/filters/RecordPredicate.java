package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;

/**
 *
 */
public interface RecordPredicate {

    boolean test(Record record, LogFilterContext ctx);

    default RecordPredicate not() {
        return new NotPredicate(this);
    }
}
