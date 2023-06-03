package com.logviewer.data2.net.server;

import com.logviewer.data2.Log;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Position;
import com.logviewer.data2.RecordList;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.Status;
import org.springframework.lang.NonNull;

public class RecordLoaderRemoteTask extends AbstractDataLoaderTask<Object> {

    private final String file;
    private final String format;
    private final Position start;
    private final boolean backward;
    private final String hash;
    private final String filter;
    private final int recordCountLimit;
    private final long sizeLimit;

    public RecordLoaderRemoteTask(String file, String format,
                           Position start, boolean backward, String hash,
                           String filter, int recordCountLimit, long sizeLimit) {
        this.file = file;
        this.format = format;
        this.start = start;
        this.backward = backward;
        this.hash = hash;
        this.filter = filter;
        this.recordCountLimit = recordCountLimit;
        this.sizeLimit = sizeLimit;
    }

    @Override
    public LogProcess createLogProcessTask(RemoteTaskContext<Object> ctx) {
        Log log = ctx.getLogService().openLog(file, LvGsonUtils.GSON.fromJson(format, LogFormat.class));

        return log.loadRecords(LvGsonUtils.GSON.fromJson(filter, RecordPredicate.class), recordCountLimit, start, backward, hash, sizeLimit, new LogDataListener() {
            @Override
            public void onData(@NonNull RecordList data) {
                ctx.send(data);
            }

            @Override
            public void onFinish(@NonNull Status status) {
                ctx.sendAndCloseChannel(status);
            }
        });
    }
}

