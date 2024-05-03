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
import java.util.stream.Collectors;

import static com.logviewer.web.session.tasks.LoadRecordTask.PAIR_COMPARATOR;

public class SearchTask extends SessionTask<SearchTask.SearchResponse> {

    private final Position start;
    private final int recordCount;
    private final boolean backward;
    private final RecordPredicate filter;
    private final SearchPattern pattern;
    private final Map<String, String> hashes;

    private final Map<LogView, LogProcess> searchers = new IdentityHashMap<>();
    private final Map<LogView, LogProcess> recordLoaders = new IdentityHashMap<>();

    private boolean finished;

    public SearchTask(LogView[] logs, Position start, int recordCount, boolean backward,
                      @NonNull SearchPattern pattern,
                      @NonNull Map<String, String> hashes, @Nullable RecordPredicate filter) {
        super(logs);

        this.start = start;
        this.recordCount = recordCount;
        this.backward = backward;
        this.pattern = pattern;
        this.filter = filter;
        this.hashes = hashes;
    }

    @Override
    public synchronized void execute(BiConsumer<SearchResponse, Throwable> consumer) {
        Map<String, Status> statuses = new HashMap<>();
        Map<String, SearchResult> resultPerLog = new HashMap<>();

        for (LogView log : logs) {
            String hash = hashes.get(log.getId());
            if (hash == null)
                continue;

            LogProcess searcher = log.createRecordSearcher(start, backward, filter, hash, recordCount, pattern,
                    searchResult -> {
                        synchronized (SearchTask.this) {
                            if (finished)
                                return;

                            resultPerLog.put(log.getId(), searchResult);
                            statuses.put(log.getId(), searchResult.getStatus());

                            if (resultPerLog.size() == searchers.size()) {
                                // All searchers returned result, process its.
                                processSearchResults(resultPerLog, statuses, consumer);
                            } else {
                                if (searchResult.isFound()) {
                                    for (Map.Entry<LogView, LogProcess> entry : searchers.entrySet()) {
                                        if (entry.getKey() != log) {
                                            Pair<LogRecord, Throwable> pair = searchResult.getData().get(searchResult.getData().size() - 1);
                                            long time = pair.getFirst().getTime();
                                            entry.getValue().setTimeLimit(LogProcess.makeTimeLimitNonStrict(backward, time));
                                        }
                                    }
                                }
                            }
                        }
                    });

            searchers.put(log, searcher);
        }

        searchers.values().forEach(LogProcess::start);
    }

    private void processSearchResults(Map<String, SearchResult> resultPerLog, Map<String, Status> statuses,
                                      BiConsumer<SearchResponse, Throwable> consumer) {
        Comparator<Pair<LogRecord, Throwable>> recordComparator = backward ? PAIR_COMPARATOR.reversed() : PAIR_COMPARATOR;

        Optional<Pair<LogRecord, Throwable>> firstOccurrence = resultPerLog.values().stream()
                .filter(SearchResult::isFound)
                .map(r -> r.getData().get(r.getData().size() - 1))
                .min(recordComparator);

        if (!firstOccurrence.isPresent()) {
            // No result found.
            finished = true;

            consumer.accept(new SearchResponse(null, statuses), null);
            return;
        }

        Pair<LogRecord, Throwable> o = firstOccurrence.get();

        List<Pair<LogRecord, Throwable>> records = resultPerLog.values().stream()
                .filter(r -> r.getStatus().getError() == null)
                .flatMap(r -> r.getData().stream())
                .filter(p -> recordComparator.compare(o, p) >= 0)
                .sorted(recordComparator)
                .collect(Collectors.toList());

        assert records.get(records.size() - 1) == o;

        if (records.size() > recordCount) {
            records.subList(0, records.size() - recordCount).clear();
            assert records.size() == recordCount;
        }

        Set<String> finishedLoaders = new HashSet<>();

        for (LogView log : searchers.keySet()) {
            if (log.getId().equals(o.getFirst().getLogId()))
                continue;

            SearchResult r = resultPerLog.get(log.getId());
            if (r.getStatus().getError() != null)
                continue;

            if (!r.isHasSkippedLine() || r.getData().isEmpty())
                continue;

            Pair<LogRecord, Throwable> lastReturnedRecord = r.getData().get(0);

            Position start;
            int maxRecordToLoad;

            if (recordComparator.compare(lastReturnedRecord, o) < 0) {
                int index = Collections.binarySearch(records, lastReturnedRecord, recordComparator);
                if (index < 0)
                    index = -index - 1;

                maxRecordToLoad = index;

                start = new Position(lastReturnedRecord.getFirst(), !backward);
            } else {
                maxRecordToLoad = recordCount - 1;
                start = new Position(o.getFirst());
            }

            String hash = hashes.get(log.getId());
            assert hash != null;

            LogProcess recordLoader = log.loadRecords(filter, maxRecordToLoad, start, !backward, hash, Long.MAX_VALUE, new LogDataListener() {
                @Override
                public void onData(@NonNull RecordList data) {
                    assert recordComparator.compare(data.get(0), lastReturnedRecord) < 0;
                    assert recordComparator.compare(data.get(data.size() - 1), lastReturnedRecord) < 0;

                    synchronized (SearchTask.this) {
                        if (finished)
                            return;

                        records.addAll(data);

                        if (logs.length > 1)
                            records.sort(recordComparator);

                        if (records.size() > recordCount) {
                            records.subList(0, records.size() - recordCount).clear();
                            assert records.size() == recordCount;
                        }

                        if (records.size() == recordCount) {
                            recordLoaders.values().forEach(l -> {
                                long time = records.get(0).getFirst().getTime();
                                l.setTimeLimit(LogProcess.makeTimeLimitNonStrict(!backward, time));
                            });
                        }
                    }
                }

                @Override
                public void onFinish(@NonNull Status status, boolean eof) {
                    synchronized (SearchTask.this) {
                        if (finished)
                            return;

                        if (finishedLoaders.add(log.getId())) {
                            if (status.getError() != null) {
                                statuses.put(log.getId(), status);
                            }

                            if (finishedLoaders.size() == recordLoaders.size()) {
                                consumer.accept(new SearchResponse(records, statuses), null);
                            }
                        }
                    }
                }
            });

            long timeLimit;
            if (records.size() == recordCount) {
                timeLimit = records.get(0).getFirst().getTime();
            }
            else {
                timeLimit = this.start.getTime();
            }
            
            recordLoader.setTimeLimit(LogProcess.makeTimeLimitNonStrict(!backward, timeLimit));

            recordLoaders.put(log, recordLoader);
        }

        if (recordLoaders.isEmpty()) {
            consumer.accept(new SearchResponse(records, statuses), null);

            return;
        }

        recordLoaders.values().forEach(LogProcess::start);
    }

    @Override
    public synchronized void cancel() {
        if (finished)
            return;

        finished = true;
        searchers.values().forEach(LogProcess::cancel);
        recordLoaders.values().forEach(LogProcess::cancel);
    }

    public class SearchResponse extends LoadNextResponse {
        private final boolean hasSkippedLine;

        SearchResponse(List<Pair<LogRecord, Throwable>> data, Map<String, Status> statuses) {
            super(data, statuses, data == null);

            if (backward && data != null)
                Collections.reverse(data);

            this.hasSkippedLine = data != null && data.size() == recordCount;
        }

        public boolean hasSkippedLine() {
            return hasSkippedLine;
        }
    }

}
