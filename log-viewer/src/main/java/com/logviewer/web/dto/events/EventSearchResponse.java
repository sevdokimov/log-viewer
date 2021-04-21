package com.logviewer.web.dto.events;

import java.util.List;

import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.session.tasks.SearchTask;

public class EventSearchResponse extends StatusHolderEvent {

    public final List<RestRecord> records;
    public final long foundIdx;
    public final boolean hasSkippedLine;
    public final long requestId;

    public EventSearchResponse(SearchTask.SearchResponse res, long stateVersion, long requestId, long foundIdx) {
        super(res.getStatuses(), stateVersion);

        records = RestRecord.fromPairList(res.getData());
        this.foundIdx = foundIdx;
        hasSkippedLine = res.hasSkippedLine();
        this.requestId = requestId;
    }

    public EventSearchResponse(SearchTask.SearchResponse res, long stateVersion, long requestId) {
        this(res, stateVersion, requestId, -1);
    }

    @Override
    public String getName() {
        return "onSearchResponse";
    }
}
