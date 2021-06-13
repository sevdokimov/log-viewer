package com.logviewer.data2;

import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class LogRecord implements Comparable<LogRecord>, Externalizable {

    /**
     * The name of generic field that contains whole record text.
     */
    public static final String WHOLE_LINE = "_";

    private String logId;

    private String message;

    /**
     * Timestamp in NANOseconds.
     */
    private long timeNanos;

    private int[] fieldPositions;

    private long start;
    private long end;

    private boolean hasMore;

    /**
     * Used by deserializer only.
     */
    public LogRecord() {

    }

    public LogRecord(@NonNull String message, long timeNanos, long start, long end, boolean hasMore, @NonNull int[] fieldPositions) {
        Utils.assertValidTimestamp(timeNanos);

        this.message = message;
        this.timeNanos = timeNanos;

        this.start = start;
        this.end = end;
        this.hasMore = hasMore;

        this.fieldPositions = fieldPositions;
    }

    public String getLogId() {
        return logId;
    }

    public LogRecord setLogId(String logId) {
        this.logId = logId;
        return this;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public long getTime() {
        return timeNanos;
    }

    public long getTimeMillis() {
        return timeNanos / 1000_000;
    }

    public boolean hasTime() {
        return timeNanos > 0;
    }

    public String getMessage() {
        return message;
    }

    public String getFieldText(int fieldIndex) {
        int i = fieldIndex * 2;

        if (fieldPositions[i] == -1)
            return null;

        return message.substring(fieldPositions[i], fieldPositions[i + 1]);
    }

    public int getFieldStart(int fieldIndex) {
        return fieldPositions[fieldIndex * 2];
    }

    public int getFieldEnd(int fieldIndex) {
        return fieldPositions[fieldIndex * 2 + 1];
    }

    public int getFieldsCount() {
        return fieldPositions.length / 2;
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public int compareTo(LogRecord o) {
        int res = Long.compare(timeNanos, o.timeNanos);
        if (res != 0)
            return res;

        res = logId.compareTo(o.logId);
        if (res != 0)
            return res;

        return Long.compare(start, o.start);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(logId);
        out.writeObject(message); // Don't use writeUTF(), it has limit to string length!!!
        out.writeLong(timeNanos);
        out.writeLong(start);
        out.writeLong(end);
        out.writeBoolean(hasMore);

        out.writeShort(fieldPositions.length);
        for (int fieldPosition : fieldPositions) {
            out.writeInt(fieldPosition);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        logId = in.readUTF();
        message = (String) in.readObject();
        timeNanos = in.readLong();
        start = in.readLong();
        end = in.readLong();
        hasMore = in.readBoolean();

        int fieldPositionsSize = in.readUnsignedShort();
        fieldPositions = new int[fieldPositionsSize];
        for (int i = 0; i < fieldPositionsSize; i++) {
            fieldPositions[i] = in.readInt();
        }
    }

    @NonNull
    public static LogRecord createUnparsedRecord(@NonNull String message, long time, long start, long end, boolean hasMore, @NonNull LogFormat logFormat) {
        int[] fieldOffsets = new int[logFormat.getFields().length * 2];
        Arrays.fill(fieldOffsets, -1);
        return new LogRecord(message, time, start, end, hasMore, fieldOffsets);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogRecord record = (LogRecord) o;
        return timeNanos == record.timeNanos && start == record.start && end == record.end && hasMore == record.hasMore
                && logId.equals(record.logId) && message.equals(record.message)
                && Arrays.equals(fieldPositions, record.fieldPositions);
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
}
