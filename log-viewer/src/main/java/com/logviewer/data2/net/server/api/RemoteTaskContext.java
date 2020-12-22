package com.logviewer.data2.net.server.api;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface RemoteTaskContext<E> extends RemoteContext {
    void send(@Nullable E o);

    void sendAndCloseChannel(@Nullable E o);

    void sendErrorAndCloseChannel(@NonNull Throwable t);
}
