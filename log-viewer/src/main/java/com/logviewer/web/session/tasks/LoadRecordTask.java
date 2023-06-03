package com.logviewer.web.session.tasks;

import com.logviewer.data2.LogRecord;
import com.logviewer.data2.LogView;
import com.logviewer.data2.Position;
import com.logviewer.data2.RecordList;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class LoadRecordTask extends SessionTask<LoadNextResponse> {

    static final Comparator<Pair<LogRecord, Throwable>> PAIR_COMPARATOR = Comparator.comparing(Pair::getFirst);

    protected final int recordCount;

    protected final RecordPredicate filter;

    @Nullable
    protected final Position start;

    protected final boolean backward;

    @Nullable
    protected final Map<String, String> hashes;

    protected final Comparator<Pair<LogRecord, Throwable>> comparator;

    protected final Map<LogView, LogProcess> loaders = new IdentityHashMap<>();

    protected final Map<String, Status> statuses = new HashMap<>();
    protected final List<Pair<LogRecord, Throwable>> data = new ArrayList<>();

    protected final Map<String, LogRecord> overLimitRecords = new HashMap<>();

    protected boolean finished;

    public LoadRecordTask(@NonNull SessionAdapter sender, @NonNull LogView[] logs, int recordCount, RecordPredicate filter,
                          @Nullable Position start, boolean backward, @Nullable Map<String, String> hashes) {
        super(sender, logs);

        this.recordCount = recordCount;
        this.start = start;
        this.hashes = hashes;
        assert recordCount > 0;

        this.backward = backward;
        this.filter = filter;
        comparator = backward ? PAIR_COMPARATOR.reversed() : PAIR_COMPARATOR;
    }

    @Override
    public synchronized void execute(BiConsumer<LoadNextResponse, Throwable> consumer) {
        LogView[] logs;
        if (hashes == null) {
            logs = this.logs;
        } else {
            // remove logs that is not present in "hashes"
            logs = Stream.of(this.logs).filter(l -> hashes.get(l.getId()) != null).toArray(LogView[]::new);
        }

        if (logs.length == 0) {
            finished = true;
            consumer.accept(new LoadNextResponse(Collections.emptyList(), Collections.emptyMap()), null);
            return;
        }
        
        for (LogView log : logs) {
            String hash = hashes == null ? null : hashes.get(log.getId());

            LogProcess loader = log.loadRecords(filter, recordCount,
                    start, backward, hash, MAX_BATCH_SIZE,
                    new MyLogDataListener(log, consumer));

            loaders.put(log, loader);
        }

        loaders.values().forEach(LogProcess::start);
    }

    @Nullable
    public Map<String, String> getHashes() {
        return hashes;
    }

    @Nullable
    public Position getStart() {
        return start;
    }

    public boolean isBackward() {
        return backward;
    }

    public RecordPredicate getFilter() {
        return filter;
    }

    @Override
    public synchronized void cancel() {
        if (finished)
            return;

        finished = true;
        loaders.values().forEach(LogProcess::cancel);
    }

    protected long getTimeLimit(LogRecord lastRecord, LogView log) {
        if (backward) {
            if (log.getId().compareTo(lastRecord.getLogId()) > 0)
                return lastRecord.getTime() - 1;

            return lastRecord.getTime();
        }

        if (log.getId().compareTo(lastRecord.getLogId()) < 0)
            return lastRecord.getTime() + 1;

        return lastRecord.getTime();
    }

    protected class MyLogDataListener implements LogDataListener {
        private final LogView log;
        private final BiConsumer<LoadNextResponse, Throwable> consumer;

        public MyLogDataListener(LogView log, BiConsumer<LoadNextResponse, Throwable> consumer) {
            this.log = log;
            this.consumer = consumer;
        }

        @Override
        public void onData(@NonNull RecordList newRecords) {
            synchronized (LoadRecordTask.this) {
                if (finished)
                    return;
                assert !statuses.containsKey(log.getId());

                LogRecord oldLastRecord = data.size() == recordCount ? data.get(recordCount - 1).getFirst() : null;

                if (loaders.size() > 1) {
                    for (Pair<LogRecord, Throwable> newRecord : newRecords) {
                        if (newRecord.getFirst().hasTime()) // Ignore records without time on log merging
                            data.add(newRecord);
                    }

                    data.sort(comparator);
                } else {
                    data.addAll(newRecords);
                }

                if (data.size() > recordCount) {
                    for (int i = recordCount; i < data.size(); i++) {
                        Pair<LogRecord, Throwable> pair = data.get(i);
                        overLimitRecords.putIfAbsent(pair.getFirst().getLogId(), pair.getFirst());
                    }

                    do {
                        data.remove(data.size() - 1);
                    } while (data.size() > recordCount);
                }

                if (data.size() == recordCount) {
                    LogRecord lastRecord = data.get(recordCount - 1).getFirst();
                    loaders.forEach((l, loader) -> {
                        if (oldLastRecord == null || getTimeLimit(oldLastRecord, l) != getTimeLimit(lastRecord, l))
                            loader.setTimeLimit(getTimeLimit(lastRecord, l));
                    });
                }
            }
        }

        @Override
        public void onFinish(@NonNull Status status) {
            synchronized (LoadRecordTask.this) {
                if (finished)
                    return;

                statuses.put(log.getId(), status);

                if (statuses.size() == loaders.size())
                    finished = true;
            }

            if (finished) {
                if (backward)
                    Collections.reverse(data);

                for (Map.Entry<String, Status> entry : statuses.entrySet()) {
                    Status s = entry.getValue();
                    String logId = entry.getKey();

                    if (s.getError() != null)
                        continue;

                    LogRecord outOfResult = overLimitRecords.get(logId);
                    if (outOfResult != null) {
                        assert backward
                                ? s.getLastRecordOffset() <= outOfResult.getStart()
                                : s.getLastRecordOffset() >= outOfResult.getStart();

                        entry.setValue(new Status(s,
                                outOfResult.getStart(), false, false, true,
                                s.getFirstRecordOffset(), (s.getFlags() & Status.FLAG_FIRST_RECORD_FILTER_MATCH) != 0));
                    }
                }

                consumer.accept(new LoadNextResponse(data, statuses), null);
            }
        }
    }
}
