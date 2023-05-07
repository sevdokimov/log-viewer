package com.logviewer.data2.net.server;

import com.logviewer.data2.Log;
import com.logviewer.data2.ParserConfig;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.formats.SimpleLogFormat;
import com.logviewer.utils.Pair;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;

public class LoadContentTask implements RemoteTask<Pair<String, Integer>> {

    private final String file;
    private final long offset;
    private final int length;
    private final Charset encoding;

    public LoadContentTask(String file, long offset, int length, Charset encoding) {
        this.file = file;
        this.offset = offset;
        this.length = length;
        this.encoding = encoding;

        if (length > ParserConfig.MAX_LINE_LENGTH)
            throw new IllegalArgumentException();
    }

    @Override
    public void start(@NonNull RemoteTaskContext<Pair<String, Integer>> ctx) {
        Log log = ctx.getLogService().openLog(file, new SimpleLogFormat(encoding));
        log.loadContent(offset, length).whenComplete((res, error) -> {
            if (error != null) {
                ctx.sendErrorAndCloseChannel(error);
            } else {
                ctx.sendAndCloseChannel(res);
            }
        });
    }

    @Override
    public void cancel() {

    }
}
