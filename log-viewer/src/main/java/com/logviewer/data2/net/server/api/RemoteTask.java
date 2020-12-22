package com.logviewer.data2.net.server.api;

import org.springframework.lang.NonNull;

import java.io.Serializable;

public interface RemoteTask<E> extends Serializable {

    void start(@NonNull RemoteTaskContext<E> ctx);

    void cancel();

}
