package com.logviewer.data2;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

public class Position implements Comparable<Position>, Serializable {

    private final String logId;

    private final long time;

    private final long o;

    public Position(String logId, long time, long o) {
        this.logId = logId;
        this.time = time;
        this.o = o;
    }

    public Position(Record record) {
        this(record, true);
    }

    public Position(Record record, boolean atStart) {
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

        if (time != position.time) return false;
        if (this.o != position.o) return false;
        return logId.equals(position.logId);
    }

    @Override
    public int hashCode() {
        int result = logId.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (int) (o ^ (o >>> 32));
        return result;
    }

    @Override
    public int compareTo(Position o) {
        int res = Long.compare(time, o.time);

        if (res == 0) {
            res = logId.compareTo(o.logId);

            if (res == 0)
                res = Long.compare(this.o, o.o);
        }

        return res;
    }

    @Override
    public String toString() {
        return logId + " - " + DateFormat.getDateTimeInstance().format(new Date(time)) + " - " + o;
    }
}
