package com.logviewer.data2.net;

import com.logviewer.data2.net.server.api.RemoteContext;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.data2.net.server.api.RemoteTaskController;
import com.logviewer.data2.net.server.msg.MessageStartTask;
import com.logviewer.data2.net.server.msg.MessageTaskCallbackCall;
import com.logviewer.data2.net.server.msg.MessageTaskChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class OutcomeConnection extends AbstractConnection {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeConnection.class);

    private final Node node;

    private final Map<Long, RemoteTaskControllerImpl> tasks = new HashMap<>();

    private long callCounter = 0;

    OutcomeConnection(Node node, AsynchronousSocketChannel socket) {
        super(socket);
        this.node = node;
    }

    protected void handleMessage(Object msg) {
        if (msg instanceof MessageTaskCallbackCall) {
            MessageTaskCallbackCall message = (MessageTaskCallbackCall) msg;

            RemoteTaskControllerImpl controller;
            synchronized (OutcomeConnection.this) {
                if (message.isTaskStopped()) {
                    controller = tasks.remove(message.getTaskId());
                }
                else {
                    controller = tasks.get(message.getTaskId());
                }
            }

            if (controller == null || controller.canceled)
                return;

            try {
                if (message.getError() != null) {
                    assert message.getEvent() == null;
                    controller.callback.accept(null, message.getError());
                } else {
                    controller.callback.accept(message.getEvent(), null);
                }
            } catch (Throwable e) {
                LOG.error("Failed to call callback", e);
            }
        }
        else {
            LOG.error("Unknown message: {}", msg);
        }
    }

    public <R> CompletableFuture<R> execute(Function<RemoteContext, R> task) {
        CompletableFuture<R> future = new CompletableFuture<>();

        RemoteTaskController<TaskWrapper<R>> taskController = startTask(new TaskWrapper<R>(task), (res, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                future.complete(res);
            }
        });

        future.whenComplete((res, e) -> {
            if (e instanceof CancellationException) {
                taskController.cancel();
            }
        });

        return future;
    }

    public synchronized <E, T extends RemoteTask<E>> RemoteTaskController<T> startTask(T task,
                                                                                       BiConsumer<E, Throwable> callback) {
        if (closed)
            throw new IllegalStateException();

        long taskId = ++callCounter;

        RemoteTaskControllerImpl<E, T> controller = new RemoteTaskControllerImpl<>(taskId, callback);

        tasks.put(taskId, controller);

        sendMessage(new MessageStartTask(taskId, task));

        return controller;
    }

    @Override
    protected void onDisconnect() {
        IOException e = new IOException("Disconnected");

        List<RemoteTaskControllerImpl> consumers;

        synchronized (this) {
            closed = true;
            consumers = new ArrayList<>(tasks.values());
            tasks.clear();
        }

        for (RemoteTaskControllerImpl controller : consumers) {
            try {
                if (!controller.canceled)
                    controller.callback.accept(null, e);
            } catch (Throwable ex) {
                LOG.error("Failed to call callback", e);
            }
        }
    }

    private static class TaskWrapper<R> implements RemoteTask<R> {

        private final Function<RemoteContext, R> task;

        private Future<?> future;

        TaskWrapper(Function<RemoteContext, R> task) {
            this.task = task;
        }

        @Override
        public void start(RemoteTaskContext<R> ctx) {
            future = ctx.getLogService().getExecutor().submit(() -> {
                try {
                    R res = task.apply(ctx);

                    if (future.isCancelled())
                        return;
                    
                    ctx.sendAndCloseChannel(res);
                } catch (Throwable e) {
                    if (future.isCancelled())
                        return;

                    ctx.sendErrorAndCloseChannel(e);
                }
            });
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }

    private class RemoteTaskControllerImpl<E, T extends RemoteTask<E>> implements RemoteTaskController<T> {

        private final long taskId;

        private final BiConsumer<E, Throwable> callback;

        private volatile boolean canceled;

        RemoteTaskControllerImpl(long taskId, BiConsumer<E, Throwable> callback) {
            this.taskId = taskId;
            this.callback = callback;
        }

        @Override
        public void alterTask(Consumer<T> modifier) {
            sendMessage(new MessageTaskChangeEvent(taskId, (Consumer) modifier));
        }

        @Override
        public void cancel() {
            canceled = true;

            sendMessage(new MessageTaskChangeEvent(taskId, null));
        }
    }
}
