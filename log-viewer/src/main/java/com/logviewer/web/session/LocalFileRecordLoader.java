package com.logviewer.web.session;

import com.logviewer.data2.*;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import com.logviewer.utils.PredicateUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LocalFileRecordLoader implements LogProcess {
    private final Supplier<Snapshot> snapshotFactory;

    private final ExecutorService executor;

    private final LogDataListener listener;

    private final Position start;

    private final RecordPredicate filter;
    private final Long timeLimitFomFilter;
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

        timeLimitFomFilter = PredicateUtils.extractTimeLimit(filter, !backward);
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

                    MyRecordPredicate predicate = new MyRecordPredicate(snapshot);

                    boolean hasMoreLine;

                    if (start == null) {
                        assert backward;
                        Long startTime = PredicateUtils.extractTimeLimit(filter, true);
                        if (startTime != null) {
                            hasMoreLine = snapshot.processFromTimeBack(startTime, predicate);
                        } else {
                            hasMoreLine = snapshot.processRecordsBack(snapshot.getSize(), false, predicate);
                        }
                    } else {
                        hasMoreLine = searchFromPosition(snapshot, predicate);
                    }

                    processedAllLined = hasMoreLine || predicate.stoppedByFilterTimeLimit;
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

    private boolean searchFromPosition(Snapshot snapshot, Predicate<Record> predicate) throws IOException {
        Long startTimeFromFilters = PredicateUtils.extractTimeLimit(filter, backward);

        int idCmp = start.getLogId().compareTo(snapshot.getLog().getId());
        if (idCmp == 0) {
            AtomicBoolean wrongDateFlag = new AtomicBoolean();
            AtomicBoolean firstRecordProcessedFlag = new AtomicBoolean();

            boolean res;

            if (backward) {
                res = snapshot.processRecordsBack(start.getLocalPosition(), true, rec -> {
                    if (startTimeFromFilters != null && rec.hasTime() && !firstRecordProcessedFlag.get()) {
                        if (rec.getTime() > startTimeFromFilters) {
                            wrongDateFlag.set(true);
                            return false;
                        }
                    }
                    firstRecordProcessedFlag.set(true);

                    return predicate.test(rec);
                });

                if (wrongDateFlag.get())
                    return snapshot.processFromTimeBack(startTimeFromFilters, predicate);
            }
            else {
                res = snapshot.processRecords(start.getLocalPosition(), true, rec -> {
                    if (startTimeFromFilters != null && rec.hasTime() && !firstRecordProcessedFlag.get()) {
                        if (rec.getTime() < startTimeFromFilters) {
                            wrongDateFlag.set(true);
                            return false;
                        }
                    }

                    firstRecordProcessedFlag.set(true);

                    return predicate.test(rec);
                });

                if (wrongDateFlag.get())
                    return snapshot.processFromTime(startTimeFromFilters, predicate);
            }

            return res;
        }

        if (backward) {
            long startTime;
            if (idCmp < 0)
                startTime = start.getTime() - 1;
            else
                startTime = start.getTime();

            if (startTimeFromFilters != null && startTime > startTimeFromFilters)
                startTime = startTimeFromFilters;

            return snapshot.processFromTimeBack(startTime, predicate);
        }
        else {
            long startTime;
            if (idCmp < 0)
                startTime = start.getTime();
            else
                startTime = start.getTime() + 1;

            if (startTimeFromFilters != null && startTime < startTimeFromFilters)
                startTime = startTimeFromFilters;

            return snapshot.processFromTime(startTime, predicate);
        }
    }

    private class MyRecordPredicate implements Predicate<Record> {

        private final LvPredicateChecker predicateChecker;
        private long readSize;
        private int recordCount;

        private boolean stoppedByFilterTimeLimit;

        public MyRecordPredicate(Snapshot snapshot) {
            this.predicateChecker = new LvPredicateChecker(snapshot.getLog());
        }

        @Override
        public boolean test(Record record) {
            if (timeLimitFomFilter != null && record.hasTime()) {
                if (backward ? record.getTime() < timeLimitFomFilter : record.getTime() > timeLimitFomFilter) {
                    stoppedByFilterTimeLimit = true;
                    return false;
                }
            }

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
