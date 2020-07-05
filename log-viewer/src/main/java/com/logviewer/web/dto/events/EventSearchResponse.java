package com.logviewer.web.dto.events;

import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.SearchTask;

import java.util.List;
import java.util.Map;

public class EventSearchResponse extends StatusHolderEvent {

    public final List<RestRecord> records;
    public final boolean hasSkippedLine;
    public final long requestId;

    public EventSearchResponse(Map<String, Status> statuses, long stateVersion, SearchTask.SearchResponse res, long requestId) {
        super(statuses, stateVersion);

        records = RestRecord.fromPairList(res.getData());
        hasSkippedLine = res.hasSkippedLine();
        this.requestId = requestId;
    }

    @Override
    public String getName() {
        return "searchResponse";
    }
}
