package com.logviewer.filters;

import com.google.gson.annotations.JsonAdapter;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;
import com.logviewer.utils.GsonNanosecondsAdapter;
import com.logviewer.utils.LvDateUtils;

import java.util.Date;

public class DatePredicate implements RecordPredicate {

    /**
     * Date in milliseconds.
     */
    private Long date;

    /**
     * Date in nanoseconds.
     */
    @JsonAdapter(GsonNanosecondsAdapter.class)
    private Long timestamp;

    private boolean greater;

    public DatePredicate() {

    }

    public DatePredicate(Date date, boolean greater) {
        this(LvDateUtils.toNanos(date.getTime()), greater);
    }

    public DatePredicate(long timestamp, boolean greater) {
        this.timestamp = timestamp;
        this.greater = greater;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        if (!record.hasTime())
            return true;

        long ts = getDate();

        if (ts == 0)
            return true;

        if (greater) {
            return record.getTime() >= ts;
        } else {
            return record.getTime() <= ts;
        }
    }

    public long getDate() {
        if (timestamp != null)
            return timestamp;

        return date == null ? 0 : LvDateUtils.toNanos(date);
    }

    public DatePredicate setDate(long timestamp) {
        this.timestamp = timestamp;
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
