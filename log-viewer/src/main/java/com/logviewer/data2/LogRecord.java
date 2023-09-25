package com.logviewer.data2;

import com.logviewer.utils.TextRange;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

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

    private Map<String, Integer> fieldNames;
    private int[] fieldPositions;

    private long start;
    private long end;

    /**
     * The length of {@link #message} in bytes. The value must be (end - start) for log records with length less than {@link ParserConfig#MAX_LINE_LENGTH}.
     * For long log records the value will be less than (end - start).
     */
    private int loadedTextLengthBytes;

    /**
     * Used by deserializer only.
     */
    public LogRecord() {

    }

    public LogRecord(@NonNull String message, long timeNanos, long start, long end, int loadedTextLengthBytes) {
        this(message, timeNanos, start, end, loadedTextLengthBytes, Utils.EMPTY_INT_ARRAY, Collections.emptyMap());
    }

    public LogRecord(@NonNull String message, long timeNanos, long start, long end, int loadedTextLengthBytes,
                     @NonNull int[] fieldPositions, @NonNull Map<String, Integer> fieldNames) {
        assert fieldPositions.length == fieldNames.size() * 2;

        Utils.assertValidTimestamp(timeNanos);

        this.message = message;
        this.timeNanos = timeNanos;

        this.start = start;
        this.end = end;
        this.loadedTextLengthBytes = loadedTextLengthBytes;

        this.fieldPositions = fieldPositions;
        this.fieldNames = fieldNames;
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
        return loadedTextLengthBytes < end - start;
    }

    public int getLoadedTextLengthBytes() {
        return loadedTextLengthBytes;
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

    public TextRange getFieldOffset(String fieldName) {
        Integer fieldIndex = fieldNames.get(fieldName);
        if (fieldIndex == null)
            return null;

        int i = fieldIndex * 2;

        if (fieldPositions[i] == -1)
            return null;

        return new TextRange(fieldPositions[i], fieldPositions[i + 1]);
    }

    public String getFieldText(String fieldName) {
        Integer fieldIndex = fieldNames.get(fieldName);
        if (fieldIndex == null)
            return null;
        
        int i = fieldIndex * 2;

        if (fieldPositions[i] == -1)
            return null;

        return message.substring(fieldPositions[i], fieldPositions[i + 1]);
    }

    public Set<String> getFieldNames() {
        return fieldNames.keySet();
    }

    /**
     * An array containing field positions. The start offset of a field with index "i" is located in getFieldPositions()[i * 2],
     * the end is in getFieldPositions()[i * 2 + 1]
     */
    public int[] getFieldPositions() {
        return fieldPositions;
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
        out.writeObject(message); // Don't use writeUTF(), it has a limit on string length!!!
        out.writeLong(timeNanos);
        out.writeLong(start);
        out.writeLong(end);
        out.writeInt(loadedTextLengthBytes);

        assert fieldNames.size() * 2 == fieldPositions.length;

        out.writeShort(fieldNames.size());

        for (int fieldPosition : fieldPositions) {
            out.writeInt(fieldPosition);
        }

        for (String fieldName : fieldNames.keySet()) {
            out.writeUTF(fieldName);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        logId = in.readUTF();
        message = (String) in.readObject();
        timeNanos = in.readLong();
        start = in.readLong();
        end = in.readLong();
        loadedTextLengthBytes = in.readInt();

        int fieldCount = in.readUnsignedShort();

        fieldPositions = new int[fieldCount * 2];
        for (int i = 0; i < fieldPositions.length; i++) {
            fieldPositions[i] = in.readInt();
        }

        fieldNames = new LinkedHashMap<>();
        for (int i = 0; i < fieldCount; i++) {
            fieldNames.put(in.readUTF(), i);
        }
    }

    @NonNull
    public static LogRecord createUnparsedRecord(@NonNull String message, long time, long start, long end, int loadedTextLengthBytes) {
        return new LogRecord(message, time, start, end, loadedTextLengthBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogRecord record = (LogRecord) o;
        return timeNanos == record.timeNanos && start == record.start && end == record.end && loadedTextLengthBytes == record.loadedTextLengthBytes
                && logId.equals(record.logId) && message.equals(record.message)
                && Arrays.equals(fieldPositions, record.fieldPositions)
                && fieldNames.keySet().equals(record.fieldNames.keySet());
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
}
