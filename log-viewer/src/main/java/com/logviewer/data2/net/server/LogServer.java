package com.logviewer.data2.net.server;

import com.logviewer.data2.LogService;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashSet;
import java.util.Set;

public class LogServer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LogServer.class);

    public static final int DEFAULT_PORT = 9595;

    private final LogService logService;

    private AsynchronousServerSocketChannel socket;

    private final int port;

    private boolean closed;

    private final Set<IncomeConnection> connections = new HashSet<>();

    public LogServer(LogService logService) {
        this(logService, DEFAULT_PORT);
    }

    public LogServer(LogService logService, int port) {
        this.logService = logService;
        this.port = port;
    }

    private synchronized void onDisconnect(IncomeConnection conn) {
        connections.remove(conn);
    }

    public synchronized void startup() throws IOException {
        assert socket == null;

        socket = AsynchronousServerSocketChannel.open();
        socket.bind(new InetSocketAddress(port));
        socket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socket, Void attachment) {
                IncomeConnection connection;

                synchronized (LogServer.this) {
                    if (closed) {
                        Utils.closeQuietly(socket);
                        return;
                    }

                    connection = new IncomeConnection(socket, logService, LogServer.this::onDisconnect);
                    connections.add(connection);
                }

                connection.init();

                LogServer.this.socket.accept(null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                synchronized (LogServer.this) {
                    if (!closed) {
                        LOG.error("Server socket failed", exc);
                        close();
                    }
                }
            }
        });
    }

    public synchronized void close() {
        if (closed)
            return;

        closed = true;

        try {
            socket.close();
        } catch (IOException e) {
            LOG.error("Failed to close log server", e);
        }

        for (IncomeConnection connection : connections.toArray(new IncomeConnection[0])) {
            try {
                connection.close();
            } catch (Exception e) {
                LOG.error("Failed to close connection", e);
            }
        }
    }
}
