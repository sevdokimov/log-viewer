package com.logviewer.data2.net.server;

import com.logviewer.data2.LogService;
import com.logviewer.data2.net.server.api.RemoteTaskContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RemoteTaskContextImpl<CALL_BACK> implements RemoteTaskContext<CALL_BACK> {

    private final LogService logService;

    private final BiConsumer<CALL_BACK, Boolean> listener;
    private final Consumer<Throwable> errorListener;

    private boolean closed;

    public RemoteTaskContextImpl(LogService logService, BiConsumer<CALL_BACK, Boolean> listener,
                                 Consumer<Throwable> errorListener) {
        this.logService = logService;
        this.listener = listener;
        this.errorListener = errorListener;
    }

    @Override
    public LogService getLogService() {
        return logService;
    }

    @Override
    public void send(@Nullable CALL_BACK o) {
        assert !closed;
        listener.accept(o, false);
    }

    @Override
    public void sendAndCloseChannel(@Nullable CALL_BACK o) {
        assert !closed;
        closed = true;

        listener.accept(o, true);
    }

    @Override
    public void sendErrorAndCloseChannel(@Nonnull Throwable t) {
        assert !closed;
        closed = true;

        errorListener.accept(t);
    }
}
