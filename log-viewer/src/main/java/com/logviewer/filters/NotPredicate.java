package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;
import org.springframework.lang.NonNull;

/**
 *
 */
public class NotPredicate implements RecordPredicate {

    private RecordPredicate delegate;

    public NotPredicate(@NonNull RecordPredicate delegate) {
        this.delegate = delegate;
    }

    public RecordPredicate getDelegate() {
        return delegate;
    }

    public void setDelegate(@NonNull RecordPredicate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        return !delegate.test(record, ctx);
    }

    @Override
    public RecordPredicate not() {
        return delegate;
    }
}
