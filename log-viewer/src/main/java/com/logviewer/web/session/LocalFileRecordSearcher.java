package com.logviewer.web.session;

import com.logviewer.data2.*;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import com.logviewer.utils.PredicateUtils;
import com.logviewer.web.session.tasks.SearchPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LocalFileRecordSearcher implements LogProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileRecordSearcher.class);

    private final Supplier<Snapshot> snapshotFactory;

    private final ExecutorService executor;
    private final Position start;
    private final boolean backward;
    private final RecordPredicate filter;
    private final Long timeLimitFomFilter;
    private final SearchPattern pattern;
    private final String hash;
    private final int recordCount;

    private final Consumer<SearchResult> listener;

    private int state = 0;

    private volatile long timeLimit = 0;


    private volatile Future<?> future;

    public LocalFileRecordSearcher(Supplier<Snapshot> snapshotFactory, ExecutorService executor,
                                   Position start, boolean backward, RecordPredicate filter, String hash, int recordCount,
                                   SearchPattern pattern, Consumer<SearchResult> listener) {
        this.snapshotFactory = snapshotFactory;
        this.executor = executor;
        this.start = start;
        this.backward = backward;
        this.filter = filter;
        this.pattern = pattern;
        this.hash = hash;
        this.recordCount = recordCount;
        this.listener = listener;

        assert recordCount > 0;

        timeLimitFomFilter = PredicateUtils.extractTimeLimit(filter, !backward);
    }

    @Override
    public synchronized void start() {
        if (state != 0)
            throw new IllegalStateException("Loader already started");

        assert future == null;

        future = executor.submit(() -> {
            try (Snapshot snapshot = snapshotFactory.get()) {
                try {
                    if (hash != null && !snapshot.isValidHash(hash))
                        throw new LogCrashedException();

                    Queue<Pair<LogRecord, Throwable>> queue = new ArrayDeque<>(recordCount);
                    boolean[] hasSkippedLined = new boolean[1];
                    boolean[] found = new boolean[1];

                    Predicate<String> matcher = pattern.matcher();

                    LvPredicateChecker predicateChecker = new LvPredicateChecker(snapshot.getLog());

                    Predicate<LogRecord> predicate = record -> {
                        if (timeLimitFomFilter != null && record.hasTime()) {
                            if (backward ? record.getTime() < timeLimitFomFilter : record.getTime() > timeLimitFomFilter)
                                return false;
                        }

                        if (!timeOk(record))
                            return false;

                        Pair<LogRecord, Throwable> restRecord = predicateChecker.applyFilter(record, filter);

                        if (restRecord != null) {
                            if (queue.size() == recordCount) {
                                hasSkippedLined[0] = true;
                                queue.remove();
                            }

                            queue.add(restRecord);

                            if (matcher.test(record.getMessage())) {
                                found[0] = true;
                                return false;
                            }
                        }

                        return true;
                    };

                    int idCmp = start.getLogId().compareTo(snapshot.getLog().getId());
                    if (idCmp == 0) {
                        if (backward)
                            snapshot.processRecordsBack(start.getLocalPosition(), true, predicate);
                        else
                            snapshot.processRecords(start.getLocalPosition(), true, predicate);
                    }
                    else {
                        if (backward) {
                            long startTime;
                            if (idCmp < 0)
                                startTime = start.getTime() - 1;
                            else
                                startTime = start.getTime();

                            snapshot.processFromTimeBack(startTime, predicate);
                        }
                        else {
                            long startTime;
                            if (idCmp < 0)
                                startTime = start.getTime();
                            else
                                startTime = start.getTime() + 1;

                            snapshot.processFromTime(startTime, predicate);
                        }
                    }

                    Status status = new Status(snapshot, -1, false, false, true, -1, false);
                    listener.accept(new SearchResult(new RecordList(queue), status, hasSkippedLined[0], found[0]));
                } catch (Throwable e) {
                    listener.accept(new SearchResult(e));
                }
            } catch (Throwable e) {
                LOG.error("Failed to load records", e);
            }
        });

        state = 1;
    }

    private boolean timeOk(LogRecord record) {
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


}
