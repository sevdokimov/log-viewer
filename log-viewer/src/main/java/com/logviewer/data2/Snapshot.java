package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

public interface Snapshot extends AutoCloseable {
    long getSize();

    long getLastModification();

    default boolean processRecords(long position, Predicate<LogRecord> consumer) throws IOException {
        return processRecords(position, false, consumer);
    }

    boolean processRecordsBack(long position, boolean fromNextLine, Predicate<LogRecord> consumer) throws IOException;

    boolean processRecords(long position, boolean fromNextLine, Predicate<LogRecord> consumer) throws IOException;

    boolean processFromTimeBack(long time, Predicate<LogRecord> consumer) throws IOException;

    boolean processFromTime(long time, Predicate<LogRecord> consumer) throws IOException;

    @Nullable
    Exception getError();

    Log getLog();

    boolean isValidHash(@NonNull String hash);

    String getHash();

    @Override
    void close();
}
