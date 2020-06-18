package com.logviewer.data2.net.server;

import com.logviewer.data2.Log;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.utils.LvGsonUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class TryReadTask implements RemoteTask<Void> {

    private final String file;
    private final String format;

    public TryReadTask(@Nonnull String file, String format) {
        this.file = file;
        this.format = format;
    }

    @Override
    public void start(@Nonnull RemoteTaskContext<Void> ctx) {
        Log log = ctx.getLogService().openLog(file, LvGsonUtils.GSON.fromJson(format, LogFormat.class));

        CompletableFuture<Throwable> future = log.tryRead();
        future.whenComplete((res, error) -> {
            if (res != null) {
                ctx.sendErrorAndCloseChannel(res);
            } else if (error != null) {
                ctx.sendErrorAndCloseChannel(error);
            } else {
                ctx.sendAndCloseChannel(null);
            }
        });
    }

    @Override
    public void cancel() {

    }
}
