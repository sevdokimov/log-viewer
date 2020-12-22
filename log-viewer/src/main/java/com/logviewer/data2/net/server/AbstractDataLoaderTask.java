package com.logviewer.data2.net.server;

import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.web.session.LogProcess;
import org.springframework.lang.NonNull;

public abstract class AbstractDataLoaderTask<T> implements RemoteTask<T> {

    protected Long initialTimeLimit;

    protected LogProcess logProcess;

    protected abstract LogProcess createLogProcessTask(RemoteTaskContext<T> ctx);

    @Override
    public final void start(@NonNull RemoteTaskContext<T> ctx) {
        assert logProcess == null;
        logProcess = createLogProcessTask(ctx);

        if (initialTimeLimit != null)
            logProcess.setTimeLimit(initialTimeLimit);

        logProcess.start();
    }

    public void setTimeLimit(long timeLimit) {
        if (logProcess != null) {
            logProcess.setTimeLimit(timeLimit);
        } else {
            initialTimeLimit = timeLimit;
        }
    }

    @Override
    public void cancel() {
        logProcess.cancel();
    }
}
