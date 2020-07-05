package com.logviewer.data2.net.server;

import com.logviewer.data2.LogService;
import com.logviewer.data2.net.AbstractConnection;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.msg.MessageStartTask;
import com.logviewer.data2.net.server.msg.MessageTaskCallbackCall;
import com.logviewer.data2.net.server.msg.MessageTaskChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class IncomeConnection extends AbstractConnection {

    private static final Logger LOG = LoggerFactory.getLogger(IncomeConnection.class);

    private final LogService logService;
    private final Consumer<IncomeConnection> disconnectListener;

    private final Map<Long, RemoteTask> tasks = new HashMap<>();

    public IncomeConnection(AsynchronousSocketChannel socket, LogService logService, Consumer<IncomeConnection> disconnectListener) {
        super(socket);
        this.logService = logService;
        this.disconnectListener = disconnectListener;
    }

    @Override
    protected void handleMessage(Object msg) {
        Class<?> msgType = msg.getClass();

        if (msgType == MessageStartTask.class) {
            MessageStartTask message = (MessageStartTask) msg;

            RemoteTask task = message.getTask();
            long taskId = message.getTaskId();
            RemoteTaskContextImpl ctx;

            synchronized (IncomeConnection.this) {
                if (closed)
                    return;

                RemoteTask oldTask = tasks.putIfAbsent(taskId, task);
                if (oldTask != null) {
                    LOG.error("Duplicated task [id={}, task={}]", taskId, task);
                    return;
                }

                ctx = new RemoteTaskContextImpl<>(logService, (e, isTaskStopped) -> {
                    synchronized (IncomeConnection.this) {
                        sendMessage(new MessageTaskCallbackCall(taskId, e, isTaskStopped));

                        if (isTaskStopped) {
                            tasks.remove(taskId);
                        }
                    }
                }, error -> {
                    synchronized (IncomeConnection.this) {
                        sendMessage(new MessageTaskCallbackCall(taskId, error));
                        tasks.remove(taskId);
                    }
                });
            }

            try {
                task.start(ctx);
            } catch (Throwable e) {
                ctx.sendErrorAndCloseChannel(e);
            }
        }
        else if (msgType == MessageTaskChangeEvent.class) {
            MessageTaskChangeEvent message = (MessageTaskChangeEvent) msg;

            synchronized (IncomeConnection.this) {
                if (closed)
                    return;

                long taskId = message.getTaskId();

                RemoteTask task = tasks.get(taskId);
                if (task == null)
                    return;

                if (message.getModifier() == null) { // Cancel task
                    tasks.remove(message.getTaskId());
                    try {
                        task.cancel();
                    } catch (Throwable e) {
                        LOG.error("Failed to call 'cancel()' method on task controller", e);
                    }
                }
                else {
                    try {
                        message.getModifier().accept(task);
                    } catch (Throwable e) {
                        LOG.error("Failed to handle control event", e);
                    }
                }
            }
        }
        else {
            throw new IllegalArgumentException("Unknown message: {}" + msg);
        }
    }

    @Override
    protected void onDisconnect() {
        synchronized (this) {
            for (RemoteTask task : tasks.values()) {
                try {
                    task.cancel();
                } catch (Throwable e) {
                    LOG.error("Failed to call 'cancel()' method on task controller", e);
                }
            }

            tasks.clear();
        }

        ForkJoinPool.commonPool().execute(() -> disconnectListener.accept(this)); // Call listener to another thread to avoid deadlock
    }

//    private class RemoteApiImpl implements RemoteApi {
//
//        @Override
//        public CompletableFuture<Triple<String, String, String>> getFormatAndId(String path) {
//            Log log = logService.openLog(path);
//            return CompletableFuture.completedFuture(Triple.create(LvGsonUtils.GSON.toJson(log.getFormat(), LogFormat.class), log.getId(), log.getHostname()));
//        }
//
//        @Override
//        public RecordLoaderChannel createRecordLoaderChannel(String file, String format, Position start, boolean backward,
//                                                             String hash,
//                                                             String filter, int recordCountLimit, long sizeLimit,
//                                                             Consumer listener) {
//            return new RecordLoaderChannelImpl(logService, file, format,
//                    start, backward, hash, filter, recordCountLimit, sizeLimit);
//        }
//
//        @Override
//        public RecordLoaderChannel createRecordSearcherChannel(String file, String format, Position start, boolean backward,
//                                                             String hash,
//                                                             String filter, int recordCountLimit, SearchPattern searchPattern,
//                                                             Consumer listener) {
//            return new RecordSearcherChannelImpl(logService, file, format,
//                    start, backward, hash, filter, recordCountLimit, searchPattern);
//        }
//    }
}
