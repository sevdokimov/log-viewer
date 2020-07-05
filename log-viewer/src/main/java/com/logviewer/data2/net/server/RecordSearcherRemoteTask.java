package com.logviewer.data2.net.server;

import com.logviewer.data2.Log;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Position;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.tasks.SearchPattern;

public class RecordSearcherRemoteTask extends AbstractDataLoaderTask<SearchResult> {

    private final String file;
    private final String format;
    private final Position start;
    private final boolean backward;
    private final String hash;
    private final String filter;
    private final int recordCountLimit;
    private final SearchPattern searchPattern;

    public RecordSearcherRemoteTask(String file, String format,
                             Position start, boolean backward, String hash,
                             String filter, int recordCountLimit, SearchPattern searchPattern) {
        this.file = file;
        this.format = format;
        this.start = start;
        this.backward = backward;
        this.hash = hash;
        this.filter = filter;
        this.recordCountLimit = recordCountLimit;
        this.searchPattern = searchPattern;
    }

    @Override
    protected LogProcess createLogProcessTask(RemoteTaskContext<SearchResult> ctx) {
        Log log = ctx.getLogService().openLog(file, LvGsonUtils.GSON.fromJson(format, LogFormat.class));

        return log.createRecordSearcher(start, backward,
                LvGsonUtils.GSON.fromJson(filter, RecordPredicate.class), hash, recordCountLimit, searchPattern,
                ctx::sendAndCloseChannel);
    }
}
