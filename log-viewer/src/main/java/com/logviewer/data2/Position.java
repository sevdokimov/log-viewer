package com.logviewer.data2;

import com.google.gson.annotations.JsonAdapter;
import com.logviewer.utils.GsonNanosecondsAdapter;
import com.logviewer.utils.LvDateUtils;
import com.logviewer.utils.Utils;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class Position implements Serializable {

    private final String logId;

    @JsonAdapter(GsonNanosecondsAdapter.class)
    private final Long time;

    private final long o;

    public Position(String logId, Date date, long o) {
        this(logId, LvDateUtils.toNanos(date), o);
    }

    public Position(String logId, long time, long o) {
        Utils.assertValidTimestamp(time);
        
        this.logId = logId;
        this.time = time;
        this.o = o;
    }

    public Position(LogRecord record) {
        this(record, true);
    }

    public Position(LogRecord record, boolean atStart) {
        this(record.getLogId(), record.getTime(), atStart ? record.getStart() : record.getEnd());
    }

    public String getLogId() {
        return logId;
    }

    public long getTime() {
        return time;
    }

    public long getLocalPosition() {
        return o;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (!Objects.equals(time, position.time)) return false;
        if (this.o != position.o) return false;
        return logId.equals(position.logId);
    }

    @Override
    public int hashCode() {
        int result = logId.hashCode();

        if (time != null)
            result = 31 * result + time.intValue();

        result = 31 * result + (int) o;
        return result;
    }

    @Override
    public String toString() {
        if (time == null) {
            return logId + " - " + o;
        } else {
            return logId + " - " + DateFormat.getDateTimeInstance().format(new Date(time)) + " - " + o;
        }
    }
}
