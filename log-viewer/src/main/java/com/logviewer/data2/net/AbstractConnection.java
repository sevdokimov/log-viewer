package com.logviewer.data2.net;

import com.logviewer.data2.net.server.Message;
import com.logviewer.utils.MessageReader;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractConnection implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnection.class);

    private final AsynchronousSocketChannel socket;

    private final MessageReader messageReader = new MessageReader();

    private List<Message> outcomeMsgQueue;

    protected boolean closed;

    public AbstractConnection(AsynchronousSocketChannel socket) {
        this.socket = socket;
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;

        closed = true;
        Utils.closeQuietly(socket);

        onDisconnect();
    }

    public synchronized boolean isOpen() {
        return !closed && socket.isOpen();
    }

    protected abstract void handleMessage(Object message) throws InvocationTargetException, IllegalAccessException;

    protected synchronized void sendMessage(Message message) {
        if (closed)
            return;

        if (outcomeMsgQueue != null) {
            outcomeMsgQueue.add(message);
        }
        else {
            ByteBuffer byteBuffer = MessageReader.serializeMessages(Collections.singletonList(message));

            outcomeMsgQueue = new ArrayList<>();

            socket.write(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (attachment.hasRemaining()) {
                        socket.write(attachment, attachment, this);
                        return;
                    }

                    synchronized (AbstractConnection.this) {
                        if (closed)
                            return;

                        try {
                            if (outcomeMsgQueue.isEmpty()) {
                                outcomeMsgQueue = null;
                            }
                            else {
                                ByteBuffer bb = MessageReader.serializeMessages(outcomeMsgQueue);
                                outcomeMsgQueue.clear();
                                socket.write(bb, bb, this);
                            }
                        } catch (Throwable e) {
                            LOG.error("Failed to send message", e);
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    synchronized (AbstractConnection.this) {
                        if (closed)
                            return;

                        LOG.error("Failed to send message", exc);
                        close();
                    }
                }
            });
        }
    }

    protected void onDisconnect() {

    }

    public void init() {
        socket.read(messageReader.getCurrentBuffer(), null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                assert result != 0;

                if (result == -1) {
                    close();
                    return;
                }

                try {
                    Object msg = messageReader.onReceive();
                    if (msg != null) {
                        handleMessage(msg);
                    }

                    socket.read(messageReader.getCurrentBuffer(), null, this);
                } catch (Throwable e) {
                    LOG.error("Failed to read message", e);
                    close();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                synchronized (AbstractConnection.this) {
                    if (closed)
                        return;

                    LOG.error("Disconnected", exc);
                    close();
                }
            }
        });

    }


}
