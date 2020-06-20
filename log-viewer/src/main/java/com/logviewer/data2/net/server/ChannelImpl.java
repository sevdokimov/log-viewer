package com.logviewer.data2.net.server;

import com.logviewer.data2.net.server.api.ChannelController;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

public abstract class ChannelImpl implements Runnable {

    private BiConsumer<Object, Boolean> listener;

    private boolean closed;

    public final void setListener(BiConsumer<Object, Boolean> listener) {
        this.listener = listener;
    }

    protected void sendMessage(@Nonnull Object o, boolean closeChannel) {
        listener.accept(o, closeChannel);
    }

    public boolean isClosed() {
        return closed;
    }

    public final void markClosed() {
        closed = true;
    }

    /**
     * Implementation for {@link ChannelController#close()}. All ChannelImpl should implement {@link ChannelController}.
     */
    public final void close() {
        throw new UnsupportedOperationException();
    }
}
