package com.logviewer.data2.net.server;

import com.logviewer.data2.Log;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import com.logviewer.data2.Snapshot;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.utils.LvGsonUtils;

import java.io.IOException;

public class LoadOneRecordTask implements RemoteTask<LogRecord> {

    private final String file;
    private final String format;
    private final long offset;

    public LoadOneRecordTask(String file, String format, long offset) {
        this.file = file;
        this.format = format;
        this.offset = offset;
    }

    @Override
    public void start(RemoteTaskContext<LogRecord> ctx) {
        Log log = ctx.getLogService().openLog(file, LvGsonUtils.GSON.fromJson(format, LogFormat.class));

        try (Snapshot snapshot = log.createSnapshot()) {
            boolean notFound = snapshot.processRecords(offset, false, r -> {
                ctx.sendAndCloseChannel(r);
                return false;
            });
            if (notFound) {
                ctx.sendAndCloseChannel(null);
            }
        } catch (IOException e) {
            ctx.sendErrorAndCloseChannel(e);
        }
    }

    @Override
    public void cancel() {

    }
}
