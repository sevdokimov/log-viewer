package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;

import javax.annotation.Nonnull;

/**
 *
 */
public class NotPredicate implements RecordPredicate {

    private RecordPredicate delegate;

    public NotPredicate(@Nonnull RecordPredicate delegate) {
        this.delegate = delegate;
    }

    public RecordPredicate getDelegate() {
        return delegate;
    }

    public void setDelegate(@Nonnull RecordPredicate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        return !delegate.test(record, ctx);
    }

    @Override
    public RecordPredicate not() {
        return delegate;
    }
}
