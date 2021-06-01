package com.logviewer.web.session.tasks;

import com.logviewer.data2.LogRecord;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.Status;

import java.util.List;
import java.util.Map;

public class LoadNextResponse {
    protected final List<Pair<LogRecord, Throwable>> data;
    protected final Map<String, Status> statuses;
    protected final boolean eof;

    LoadNextResponse(List<Pair<LogRecord, Throwable>> data, Map<String, Status> statuses, boolean eof) {
        this.data = data;
        this.statuses = statuses;
        this.eof = eof;
    }

    public List<Pair<LogRecord, Throwable>> getData() {
        return data;
    }

    public Map<String, Status> getStatuses() {
        return statuses;
    }

    public boolean hasNextLine() {
        return !eof;
    }
}
