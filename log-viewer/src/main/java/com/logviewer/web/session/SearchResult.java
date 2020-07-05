package com.logviewer.web.session;

import com.logviewer.data2.RecordList;

import java.io.Serializable;

public class SearchResult implements Serializable {
    private final RecordList data;
    private final Status status;
    private final boolean hasSkippedLine;
    private final boolean found;

    public SearchResult(Throwable error) {
        this(null, new Status(error), false, false);
    }

    public SearchResult(RecordList data, Status status, boolean hasSkippedLine, boolean found) {
        this.data = data;
        this.status = status;
        this.hasSkippedLine = hasSkippedLine;
        this.found = found;
    }

    public RecordList getData() {
        return data;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isHasSkippedLine() {
        return hasSkippedLine;
    }

    public boolean isFound() {
        return found;
    }
}
