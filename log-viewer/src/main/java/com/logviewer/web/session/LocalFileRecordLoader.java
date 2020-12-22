package com.logviewer.web.session;

import com.logviewer.data2.*;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LocalFileRecordLoader implements LogProcess {
    private final Supplier<Snapshot> snapshotFactory;

    private final ExecutorService executor;

    private final LogDataListener listener;

    private final Position start;

    private final RecordPredicate filter;
    private final int recordCountLimit;
    private final boolean backward;

    private final long sizeLimit;

    private final String hash;

    private int state = 0;

    private volatile long timeLimit = 0;

    private volatile Future<?> future;

    public LocalFileRecordLoader(Supplier<Snapshot> snapshotFactory, @NonNull ExecutorService executor,
                                 LogDataListener listener,
                                 @Nullable Position start, RecordPredicate filter, boolean backward,
                                 int recordCountLimit, long sizeLimit, @Nullable String hash) {
        this.snapshotFactory = snapshotFactory;
        this.executor = executor;
        this.listener = listener;
        this.start = start;
        this.filter = filter;
        this.recordCountLimit = recordCountLimit;
        this.backward = backward;
        this.sizeLimit = sizeLimit;
        this.hash = hash;
    }

    @Override
    public synchronized void start() {
        if (state != 0)
            throw new IllegalStateException("Loader already started");

        assert future == null;

        future = executor.submit(() -> {
            try (Snapshot snapshot = snapshotFactory.get()) {
                boolean processedAllLined;
                Status status;

                try {
                    if (hash != null && !snapshot.isValidHash(hash))
                        throw new LogCrashedException();

                    Predicate<Record> predicate = new MyRecordPredicate(snapshot);

                    if (start == null) {
                        processedAllLined = snapshot.processRecordsBack(snapshot.getSize(), false, new MyRecordPredicate(snapshot));
                    } else {
                        processedAllLined = searchFromPosition(snapshot, predicate);
                    }

                    status = new Status(snapshot);
                } catch (Throwable e) {
                    processedAllLined = false;
                    status = new Status(e);
                }

                listener.onFinish(status, processedAllLined);
            }
        });

        state = 1;
    }

    private boolean timeOk(Record record) {
        long timeLimit = this.timeLimit;
        if (timeLimit <= 0)
            return true;

        long time = record.getTime();
        if (time <= 0)
            return true;

        if (backward) {
            return time > timeLimit;
        }
        else {
            return time < timeLimit;
        }
    }

    @Override
    public synchronized void cancel() {
        if (state == 0)
            throw new IllegalStateException("Loader is not started");

        if (state == 1) {
            future.cancel(true);
            state = 2;
        }
    }

    @Override
    public void setTimeLimit(long limit) {
        this.timeLimit = limit;
    }

    private boolean searchFromPosition(Snapshot snapshot, Predicate<Record> predicate) throws IOException, LogCrashedException {
        int idCmp = start.getLogId().compareTo(snapshot.getLog().getId());
        if (idCmp == 0) {
            if (backward)
                return snapshot.processRecordsBack(start.getLocalPosition(), true, predicate);
            else
                return snapshot.processRecords(start.getLocalPosition(), true, predicate);
        }

        if (backward) {
            long startTime;
            if (idCmp < 0)
                startTime = start.getTime() - 1;
            else
                startTime = start.getTime();

            return snapshot.processFromTimeBack(startTime, predicate);
        }
        else {
            long startTime;
            if (idCmp < 0)
                startTime = start.getTime();
            else
                startTime = start.getTime() + 1;

            return snapshot.processFromTime(startTime, predicate);
        }
    }

    private class MyRecordPredicate implements Predicate<Record> {

        private final LvPredicateChecker predicateChecker;
        private long readSize;
        private int recordCount;

        public MyRecordPredicate(Snapshot snapshot) {
            this.predicateChecker = new LvPredicateChecker(snapshot.getLog());
        }

        @Override
        public boolean test(Record record) {
            if (!timeOk(record))
                return false;

            Pair<Record, Throwable> restRecord = predicateChecker.applyFilter(record, filter);

            if (restRecord != null) {
                recordCount++;
                readSize += record.getMessage().length();

                listener.onData(new RecordList(restRecord));
            }

            return recordCount < recordCountLimit && readSize < sizeLimit;
        }
    }

}
