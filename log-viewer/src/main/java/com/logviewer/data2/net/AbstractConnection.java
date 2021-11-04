package com.logviewer.data2.net;

import com.logviewer.data2.net.server.Message;
import com.logviewer.utils.MessageReader;
import com.logviewer.utils.OpenByteArrayOutputStream;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public abstract class AbstractConnection implements AutoCloseable {

    private static final int OUTCOME_BUFFER_SIZE = 1024*1024;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnection.class);

    private final AsynchronousSocketChannel socket;

    private final MessageReader messageReader = new MessageReader();

    private OpenByteArrayOutputStream outcomeMsgQueue;

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

    protected abstract void handleMessage(Object message);

    protected synchronized void sendMessage(Message message) {
        WriteCompletionHandler handler = new WriteCompletionHandler();

        try {
            while (true) {
                if (closed)
                    return;

                if (outcomeMsgQueue != null) {
                    if (outcomeMsgQueue.size() >= OUTCOME_BUFFER_SIZE) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeInterruptedException(e);
                        }
                        continue;
                    }

                    MessageReader.serializeMessages(outcomeMsgQueue, message);
                    break;
                }

                OpenByteArrayOutputStream buff = new OpenByteArrayOutputStream();
                MessageReader.serializeMessages(buff, message);

                outcomeMsgQueue = new OpenByteArrayOutputStream();

                ByteBuffer byteBuffer = ByteBuffer.wrap(buff.getBuffer(), 0, buff.size());

                socket.write(byteBuffer, byteBuffer, handler);
                break;
            }
        } catch (IOException e) {
            handler.failed(e, ByteBuffer.allocate(0));
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


    private class WriteCompletionHandler implements java.nio.channels.CompletionHandler<Integer, ByteBuffer> {

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            if (attachment.hasRemaining()) {
                socket.write(attachment, attachment, this);
                return;
            }

            synchronized (AbstractConnection.this) {
                AbstractConnection.this.notifyAll();

                if (closed)
                    return;

                try {
                    if (outcomeMsgQueue.size() == 0) {
                        outcomeMsgQueue = null;
                    }
                    else {
                        ByteBuffer bb = ByteBuffer.wrap(outcomeMsgQueue.getBuffer(), 0, outcomeMsgQueue.size());
                        outcomeMsgQueue = new OpenByteArrayOutputStream();
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
    }
}
