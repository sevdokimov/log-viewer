package com.logviewer.data2;

import javax.annotation.Nonnull;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class Record implements Comparable<Record>, Externalizable {

    /**
     * The name of generic field that contains whole record text.
     */
    public static final String WHOLE_LINE = "_";

    private String logId;

    private String message;

    private long time;

    private int[] fieldPositions;

    private long start;
    private long end;

    private boolean hasMore;

    /**
     * Used by deserializer only.
     */
    public Record() {

    }

    public Record(@Nonnull String message, long time, long start, long end, boolean hasMore, @Nonnull int[] fieldPositions) {
        this.message = message;
        this.time = time;

        this.start = start;
        this.end = end;
        this.hasMore = hasMore;

        this.fieldPositions = fieldPositions;
    }

    public String getLogId() {
        return logId;
    }

    public Record setLogId(String logId) {
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
        return time;
    }

    public boolean hasTime() {
        return time > 0;
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
    public int compareTo(Record o) {
        int res = Long.compare(time, o.time);
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
        out.writeLong(time);
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
        time = in.readLong();
        start = in.readLong();
        end = in.readLong();
        hasMore = in.readBoolean();

        int fieldPositionsSize = in.readUnsignedShort();
        fieldPositions = new int[fieldPositionsSize];
        for (int i = 0; i < fieldPositionsSize; i++) {
            fieldPositions[i] = in.readInt();
        }
    }

    @Nonnull
    public static Record createUnparsedRecord(@Nonnull String message, long time, long start, long end, boolean hasMore, @Nonnull LogFormat logFormat) {
        int[] fieldOffsets = new int[logFormat.getFields().length * 2];
        Arrays.fill(fieldOffsets, -1);
        return new Record(message, time, start, end, hasMore, fieldOffsets);
    }
}
