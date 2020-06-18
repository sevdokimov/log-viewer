package com.logviewer.data2.net;

import com.logviewer.data2.*;
import com.logviewer.data2.net.server.AbstractDataLoaderTask;
import com.logviewer.data2.net.server.RecordLoaderRemoteTask;
import com.logviewer.data2.net.server.RecordSearcherRemoteTask;
import com.logviewer.data2.net.server.TryReadTask;
import com.logviewer.data2.net.server.api.RemoteTaskController;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Destroyer;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.SearchPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RemoteLog implements LogView {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLog.class);

    private final String logId;
    private final String hostname;
    private final LogPath path;
    private final Node node;
    private final LogFormat format;

    private final String serializedFormat;

    private final RemoteNodeService remoteNodeService;
    private final RemoteLogChangeListenerService remoteLogChangeListenerService;

    public RemoteLog(@Nonnull LogPath path, @Nonnull LogFormat format, @Nonnull String logId, @Nonnull String hostname,
                     @Nonnull RemoteNodeService remoteNodeService, RemoteLogChangeListenerService remoteLogChangeListenerService) {
        this.path = path;
        node = path.getNode();
        assert node != null;
        this.remoteNodeService = remoteNodeService;
        this.remoteLogChangeListenerService = remoteLogChangeListenerService;

        this.logId = logId;
        this.format = format;
        this.hostname = hostname;

        this.serializedFormat = LvGsonUtils.GSON.toJson(format, LogFormat.class);
    }

    @Override
    public String getId() {
        return logId;
    }

    @Override
    public LogPath getPath() {
        return path;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public LogFormat getFormat() {
        return format;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public LogProcess loadRecords(RecordPredicate filter, int recordCount, Position start, boolean backward, String hash, long sizeLimit, @Nonnull LogDataListener listener) {
        return new RemoteLogProcess<>(new RecordLoaderRemoteTask(path.getFile(), serializedFormat, start, backward, hash,
                LvGsonUtils.GSON.toJson(filter, RecordPredicate.class), recordCount, sizeLimit), (o, error) -> {
            if (error != null) {
                listener.onFinish(new Status(error), true);
                return;
            }

            if (o instanceof RecordList) {
                listener.onData((RecordList) o);
            } else if (o instanceof Pair) {
                Pair<Status, Boolean> pair = (Pair<Status, Boolean>) o;

                listener.onFinish(pair.getFirst(), pair.getSecond());
            } else if (o instanceof Throwable) {
                listener.onFinish(new Status((Throwable) o), false);
            } else {
                LOG.error("Unexpected message {}", o);
            }
        });
    }

    @Override
    public LogProcess createRecordSearcher(Position start, boolean backward, RecordPredicate recordPredicate,
                                           String hash, int recordCount, SearchPattern searchPattern, Consumer<SearchResult> listener) {
        return new RemoteLogProcess<>(new RecordSearcherRemoteTask(path.getFile(), serializedFormat,
                start, backward, hash, LvGsonUtils.GSON.toJson(recordPredicate, RecordPredicate.class),
                recordCount, searchPattern),
                (o, error) -> {
                    if (error != null) {
                        listener.accept(new SearchResult(error));
                        return;
                    }

                    listener.accept(o);
                });
    }

    @Override
    public Destroyer addChangeListener(Consumer<FileAttributes> changeListener) {
        return remoteLogChangeListenerService.addListener(path, changeListener);
    }

    @Override
    public CompletableFuture<Throwable> tryRead() {
        CompletableFuture<Throwable> res = new CompletableFuture<>();

        remoteNodeService.startTask(node, new TryReadTask(path.getFile(), serializedFormat), (aVoid, error) -> res.complete(error));

        return res;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    private class RemoteLogProcess<E, T extends AbstractDataLoaderTask<E>> implements LogProcess {
        private final RemoteTaskController<T> controller;

        RemoteLogProcess(T task, BiConsumer<E, Throwable> callback) {
            controller = remoteNodeService.createTask(node, task, callback);
        }

        @Override
        public void start() {
            remoteNodeService.startTask(controller);
        }

        @Override
        public void setTimeLimit(long timeLimit) {
            controller.alterTask((Consumer<T> & Serializable) task -> {
                task.setTimeLimit(timeLimit);
            });
        }

        @Override
        public void cancel() {
            controller.cancel();
        }
    }
}
