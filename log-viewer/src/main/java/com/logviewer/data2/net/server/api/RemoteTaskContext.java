package com.logviewer.data2.net.server.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RemoteTaskContext<E> extends RemoteContext {
    void send(@Nullable E o);

    void sendAndCloseChannel(@Nullable E o);

    void sendErrorAndCloseChannel(@Nonnull Throwable t);
}
