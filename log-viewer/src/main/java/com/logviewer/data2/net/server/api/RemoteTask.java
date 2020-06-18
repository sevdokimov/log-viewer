package com.logviewer.data2.net.server.api;

import javax.annotation.Nonnull;
import java.io.Serializable;

public interface RemoteTask<E> extends Serializable {

    void start(@Nonnull RemoteTaskContext<E> ctx);

    void cancel();

}
