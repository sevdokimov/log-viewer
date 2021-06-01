package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;

public class DatePredicate implements RecordPredicate {

    private long date;
    private boolean greater;

    public DatePredicate() {

    }

    public DatePredicate(long date, boolean greater) {
        this.date = date;
        this.greater = greater;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        if (!record.hasTime())
            return true;

        if (date == 0)
            return true;

        if (greater) {
            return record.getTime() >= date;
        } else {
            return record.getTime() <= date;
        }
    }

    public long getDate() {
        return date;
    }

    public DatePredicate setDate(long date) {
        this.date = date;
        return this;
    }

    public boolean isGreater() {
        return greater;
    }

    public DatePredicate setGreater(boolean greater) {
        this.greater = greater;
        return this;
    }
}
