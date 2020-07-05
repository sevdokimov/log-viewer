package com.logviewer.data2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Predicate;

public interface Snapshot extends AutoCloseable {
    long getSize();

    long getLastModification();

    default boolean processRecords(long position, Predicate<Record> consumer) throws IOException, LogCrashedException {
        return processRecords(position, false, consumer);
    }

    boolean processRecordsBack(long position, boolean fromNextLine, Predicate<Record> consumer) throws IOException, LogCrashedException;

    boolean processRecords(long position, boolean fromNextLine, Predicate<Record> consumer) throws IOException, LogCrashedException;

    boolean processFromTimeBack(long time, Predicate<Record> consumer) throws IOException, LogCrashedException;

    boolean processFromTime(long time, Predicate<Record> consumer) throws IOException, LogCrashedException;

    @Nullable
    Exception getError();

    Log getLog();

    boolean isValidHash(@Nonnull String hash) throws LogCrashedException;

    String getHash();

    @Override
    void close();
}
