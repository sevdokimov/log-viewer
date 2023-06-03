package com.logviewer.web.session.tasks;

import com.logviewer.data2.LogRecord;
import com.logviewer.utils.Pair;
import com.logviewer.web.session.Status;

import java.util.List;
import java.util.Map;

public class LoadNextResponse {
    protected final List<Pair<LogRecord, Throwable>> data;
    protected final Map<String, Status> statuses;

    LoadNextResponse(List<Pair<LogRecord, Throwable>> data, Map<String, Status> statuses) {
        this.data = data;
        this.statuses = statuses;
    }

    public List<Pair<LogRecord, Throwable>> getData() {
        return data;
    }

    public Map<String, Status> getStatuses() {
        return statuses;
    }
}
