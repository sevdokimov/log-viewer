package com.logviewer.data2.net;

import com.google.common.base.Throwables;
import com.logviewer.data2.net.server.LogViewerBackdoorServer;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskController;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.Utils;
import com.logviewer.utils.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RemoteNodeService implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeService.class);

    private final Map<Node, CompletableFuture<OutcomeConnection>> connections = new HashMap<>();

    private boolean closed;

    public synchronized CompletableFuture<OutcomeConnection> getNodeConnection(@Nonnull Node node) {
        if (closed)
            throw new IllegalStateException("Server is closed");

        while (true) {
            CompletableFuture<OutcomeConnection> future = connections.get(node);
            if (future != null) {
                if (!future.isDone())
                    return future;

                OutcomeConnection res;

                try {
                    res = future.get();
                } catch (InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                } catch (ExecutionException e) {
                    throw Throwables.propagate(e.getCause());
                }

                if (res.isOpen())
                    return future;
            }

            // Try to create new connection.
            CompletableFuture<OutcomeConnection> newFuture = new CompletableFuture<>();

            if (future == null) {
                if (connections.putIfAbsent(node, newFuture) != null)
                    continue;
            }
            else {
                if (!connections.replace(node, future, newFuture))
                    continue;
            }

            try {
                AsynchronousSocketChannel socket = AsynchronousSocketChannel.open();

                int port = node.getPort() == null ? LogViewerBackdoorServer.DEFAULT_PORT : node.getPort();

                socket.connect(new InetSocketAddress(node.getHost(), port), newFuture, new CompletionHandler<Void, CompletableFuture<OutcomeConnection>>() {
                    @Override
                    public void completed(Void result, CompletableFuture<OutcomeConnection> newFuture) {
                        synchronized (RemoteNodeService.this) {
                            if (closed)
                                Utils.closeQuietly(socket);
                            else {
                                OutcomeConnection connection = new OutcomeConnection(node, socket);
                                connection.init();
                                newFuture.complete(connection);
                            }
                        }


                    }

                    @Override
                    public void failed(Throwable exc, CompletableFuture<OutcomeConnection> newFuture) {
                        synchronized (RemoteNodeService.this) {
                            if (!closed) {
                                connections.remove(node, newFuture);
                                newFuture.completeExceptionally(exc);
                            }
                        }
                    }
                });
            } catch (Throwable e) {
                assert connections.get(node) == newFuture;
                connections.remove(node, newFuture);
                newFuture.completeExceptionally(e);
                return newFuture;
            }
        }
    }

    public <E, T extends RemoteTask<E>> RemoteTaskController<T> startTask(@Nonnull Node node,
                                                                                       @Nonnull T task,
                                                                                       @Nonnull BiConsumer<E, Throwable> callback) {
        RemoteTaskControllerImpl<E, T> controller = new RemoteTaskControllerImpl<>(node, task, callback);
        controller.start();
        return controller;
    }

    public <E, T extends RemoteTask<E>> RemoteTaskController<T> createTask(@Nonnull Node node,
                                                                           @Nonnull T task,
                                                                           @Nonnull BiConsumer<E, Throwable> callback) {
        return new RemoteTaskControllerImpl<>(node, task, callback);
    }

    public void startTask(RemoteTaskController<?> notStartedTask) {
        ((RemoteTaskControllerImpl)notStartedTask).start();
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (closed)
                return;

            closed = true;
        }

        for (CompletableFuture<OutcomeConnection> future : connections.values()) {
            if (future.isDone()) {
                try {
                    future.get().close();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                future.completeExceptionally(new IOException(getClass().getSimpleName() + " server closed"));
            }
        }
    }

    private class RemoteTaskControllerImpl<E, T extends RemoteTask<E>> implements RemoteTaskController<T> {

        private Node node;
        private T task;

        private final BiConsumer<E, Throwable> callback;

        private RemoteTaskController<T> controller;

        private boolean canceled;

        RemoteTaskControllerImpl(@Nonnull Node node, @Nonnull T task, @Nonnull BiConsumer<E, Throwable> callback) {
            this.node = node;
            this.task = task;
            this.callback = callback;
        }

        public void start() {
            Node node;
            synchronized (this) {
                if (this.node == null)
                    throw new IllegalStateException("Task already started");

                node = this.node;
                this.node = null;
            }

            getNodeConnection(node).whenComplete(Wrappers.of(LOG, (conn, e) -> {
                if (e != null) {
                    callback.accept(null, e);
                } else {
                    synchronized (this) {
                        if (canceled)
                            return;

                        RemoteTaskController<T> c = conn.startTask(task, callback);

                        assert controller == null;
                        controller = c;
                        task = null;
                    }
                }
            }));
        }

        @Override
        public void alterTask(Consumer<T> modifier) {
            synchronized (this) {
                if (controller == null) {
                    assert task != null;
                    modifier.accept(task);
                    return;
                }
            }

            controller.alterTask(modifier);
        }

        @Override
        public void cancel() {
            synchronized (this) {
                if (controller == null) {
                    canceled = true;
                    return;
                }
            }

            controller.cancel();
        }
    }
}
