package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Destroyer;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.tasks.SearchPattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LogView {

    String getId();

    LogPath getPath();

    String getHostname();

    LogFormat getFormat();

    boolean isConnected();

    LogProcess loadRecords(RecordPredicate filter, int recordCount,
                           @Nullable Position start, boolean backward, @Nullable String hash, long sizeLimit,
                           @Nonnull LogDataListener loadListener);

    LogProcess createRecordSearcher(Position start, boolean backward, RecordPredicate recordPredicate,
                                        String hash, int recordCount, SearchPattern searchPattern,
                                        Consumer<SearchResult> listener);


    Destroyer addChangeListener(Consumer<FileAttributes> changeListener);

    CompletableFuture<Throwable> tryRead();
}
