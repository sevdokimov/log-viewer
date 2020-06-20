package com.logviewer.data2.net.server.api;

import java.util.function.Consumer;

public interface RemoteTaskController<T extends RemoteTask> {

    void alterTask(Consumer<T> modifier);

    void cancel();

}
