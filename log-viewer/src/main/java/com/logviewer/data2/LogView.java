package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Destroyer;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.tasks.SearchPattern;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LogView {

    String getId();

    LogPath getPath();

    String getHostname();

    LogFormat getFormat();

    boolean isConnected();

    LogProcess loadRecords(RecordPredicate filter, int recordCount,
                           @Nullable Position start, Position stop, boolean backward, @Nullable String hash, long sizeLimit,
                           @NonNull LogDataListener loadListener);

    LogProcess createRecordSearcher(@NonNull Position start, boolean backward, RecordPredicate recordPredicate,
                                    @Nullable String hash, int recordCount, @NonNull SearchPattern searchPattern,
                                    @NonNull Consumer<SearchResult> listener);


    @Nullable
    Destroyer addChangeListener(Consumer<FileAttributes> changeListener);

    CompletableFuture<Throwable> tryRead();

    /**
     * Reads a piece of the log.
     * @param offset The offset in the log.
     * @param length The number of bytes to read. The actual read piece may be less than 'length'.
     * @return A pair of the read text and the actual size of the text in bytes.
     */
    CompletableFuture<Pair<String, Integer>> loadContent(long offset, int length);
}
