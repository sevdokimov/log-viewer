package com.logviewer.data2.net.server;

import com.logviewer.data2.LogService;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashSet;
import java.util.Set;

public class LogViewerBackdoorServer implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewerBackdoorServer.class);

    public static final int DEFAULT_PORT = 9595;

    @Autowired
    private LogService logService;

    @Value("${log-viewer.backdoor_server.port:" + DEFAULT_PORT + '}')
    private int port;
    @Value("${log-viewer.server.interface:}")
    private String serverInterface;

    private AsynchronousServerSocketChannel socket;

    private boolean closed;

    private final Set<IncomeConnection> connections = new HashSet<>();

    private synchronized void onDisconnect(IncomeConnection conn) {
        connections.remove(conn);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        startup();
    }

    public synchronized void startup() throws IOException {
        assert socket == null;

        if (port <= 0)
            return;
        
        InetSocketAddress socketAddress;

        if (!serverInterface.isEmpty()) {
            socketAddress = new InetSocketAddress(serverInterface, port);
        } else {
            socketAddress = new InetSocketAddress(port);
        }

        socket = AsynchronousServerSocketChannel.open();
        socket.bind(socketAddress);
        socket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socket, Void attachment) {
                IncomeConnection connection;

                synchronized (LogViewerBackdoorServer.this) {
                    if (closed) {
                        Utils.closeQuietly(socket);
                        return;
                    }

                    connection = new IncomeConnection(socket, logService, LogViewerBackdoorServer.this::onDisconnect);
                    connections.add(connection);
                }

                connection.init();

                LogViewerBackdoorServer.this.socket.accept(null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                synchronized (LogViewerBackdoorServer.this) {
                    if (!closed) {
                        LOG.error("Server socket failed", exc);
                        destroy();
                    }
                }
            }
        });
    }

    @Override
    public synchronized void destroy() {
        if (closed)
            return;

        closed = true;

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.error("Failed to close log server", e);
            }
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
