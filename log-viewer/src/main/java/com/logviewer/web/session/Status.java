package com.logviewer.web.session;

import com.logviewer.data2.Snapshot;
import org.springframework.lang.NonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Status implements Externalizable {

    public static final int FLAG_EOF = 1;
    public static final int FLAG_LAST_RECORD_FILTER_TIME_LIMIT = 2;
    public static final int FLAG_LAST_RECORD_FILTER_MATCH = 4;
    public static final int FLAG_FIRST_RECORD_FILTER_MATCH = 8;

    private Throwable error;

    private String hash;
    private long size;
    private long lastModification;

    private long lastRecordOffset;
    private long firstRecordOffset;
    
    private int flags;

    /**
     * Used by deserializer only.
     */
    public Status() {

    }

    public Status(Throwable error) {
        this.error = error;
    }

    public Status(Snapshot snapshot, long lastRecordOffset, boolean eof, boolean lastRecordFilterTimeLimit, boolean lastRecordFiltersMatch,
                  long firstRecordOffset, boolean firstRecordFiltersMatch) {
        this(snapshot.getHash(), snapshot.getSize(), snapshot.getLastModification(), lastRecordOffset, eof,
                lastRecordFilterTimeLimit, lastRecordFiltersMatch,
                firstRecordOffset, firstRecordFiltersMatch);
    }

    public Status(@NonNull Status status, long lastRecordOffset,
                  boolean eof, boolean lastRecordFilterTimeLimit, boolean lastRecordFiltersMatch,
                  long firstRecordOffset, boolean firstRecordFiltersMatch) {
        this(status.getHash(), status.getSize(), status.getLastModification(),
                lastRecordOffset, eof, lastRecordFilterTimeLimit, lastRecordFiltersMatch,
                firstRecordOffset, firstRecordFiltersMatch);
    }

    public Status(String hash, long size, long lastModification,
                  long lastRecordOffset, boolean eof, boolean lastRecordFilterTimeLimit, boolean lastRecordFiltersMatch,
                  long firstRecordOffset, boolean firstRecordFiltersMatch) {
        this.hash = hash;
        this.size = size;
        this.lastModification = lastModification;

        this.lastRecordOffset = lastRecordOffset;

        this.firstRecordOffset = firstRecordOffset;

        if (lastRecordFilterTimeLimit) {
            assert eof;
            assert !lastRecordFiltersMatch;
        }

        this.flags = (eof ? FLAG_EOF : 0)
                + (lastRecordFilterTimeLimit ? FLAG_LAST_RECORD_FILTER_TIME_LIMIT : 0)
                + (lastRecordFiltersMatch ? FLAG_LAST_RECORD_FILTER_MATCH : 0)
                + (firstRecordFiltersMatch ? FLAG_FIRST_RECORD_FILTER_MATCH : 0);
    }

    public Throwable getError() {
        return error;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public long getLastModification() {
        return lastModification;
    }

    public long getLastRecordOffset() {
        return lastRecordOffset;
    }

    public long getFirstRecordOffset() {
        return firstRecordOffset;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isEof() {
        return (flags & FLAG_EOF) != 0;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(error);
        if (error == null) {
            out.writeUTF(hash);
            out.writeLong(size);
            out.writeLong(lastModification);
        }

        out.writeLong(lastRecordOffset);
        out.writeLong(firstRecordOffset);
        out.write(flags);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        error = (Throwable) in.readObject();
        if (error == null) {
            hash = in.readUTF();
            size = in.readLong();
            lastModification = in.readLong();
        }

        lastRecordOffset = in.readLong();
        firstRecordOffset = in.readLong();
        flags = in.readByte();
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Status that = (Status) o;
//
//        return size == that.size && lastModification == that.lastModification
//                && Objects.equals(error, that.error)
//                && Objects.equals(hash, that.hash)
//                && lastRecordOffset == that.lastRecordOffset
//                && firstRecordOffset == that.firstRecordOffset
//                && flags == that.flags;
//    }

//    @Override
//    public int hashCode() {
//        return (int)size + ((int)lastRecordOffset) * 31;
//    }
}
