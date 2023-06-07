package com.logviewer.data2.net;

import com.logviewer.data2.FileAttributes;
import com.logviewer.data2.LogView;
import com.logviewer.data2.Position;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Destroyer;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.SearchPattern;
import org.springframework.lang.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class BrokenLog implements LogView {

    protected final Throwable error;

    public BrokenLog(Throwable error) {
        this.error = error;
    }

    @Override
    public final boolean isConnected() {
        return false;
    }

    @Override
    public LogProcess loadRecords(RecordPredicate filter, int recordCount, Position start, Position stop, boolean backward, String hash, long sizeLimit, @NonNull LogDataListener listener) {
        return new DummyLogProcess(() -> listener.onFinish(new Status(error)));
    }

    @Override
    public LogProcess createRecordSearcher(@NonNull Position start, boolean backward, RecordPredicate recordPredicate,
                                           @NonNull String hash, int recordCount, @NonNull SearchPattern searchPattern,
                                           @NonNull Consumer<SearchResult> listener) {
        return new DummyLogProcess(() -> listener.accept(new SearchResult(error)));
    }

    @Override
    public CompletableFuture<Throwable> tryRead() {
        return CompletableFuture.completedFuture(error);
    }

    @Override
    public CompletableFuture<Pair<String, Integer>> loadContent(long offset, int length) {
        CompletableFuture<Pair<String, Integer>> res = new CompletableFuture<>();
        res.completeExceptionally(error);
        return res;
    }

    private static class DummyLogProcess implements LogProcess {
        private final Runnable onStart;

        DummyLogProcess(Runnable onStart) {
            this.onStart = onStart;
        }

        @Override
        public void setTimeLimit(long timeLimit) {

        }

        @Override
        public void start() {
            onStart.run();
        }

        @Override
        public void cancel() {

        }
    }

    @Override
    public Destroyer addChangeListener(Consumer<FileAttributes> changeListener) {
        return () -> {};
    }
}
