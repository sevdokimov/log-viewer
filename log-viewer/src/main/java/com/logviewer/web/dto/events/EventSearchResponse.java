package com.logviewer.web.dto.events;

import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.session.tasks.LoadNextResponse;
import com.logviewer.web.session.tasks.SearchTask;

import java.util.ArrayList;
import java.util.List;

public class EventSearchResponse extends StatusHolderEvent {

    public final List<RestRecord> records;
    public final long foundIdx;
    public final boolean hasSkippedLine;
    public final long requestId;
    public final boolean hasNextLine;

    public EventSearchResponse(SearchTask.SearchResponse res, long stateVersion, long requestId, long foundIdx) {
        super(res.getStatuses(), stateVersion);

        records = RestRecord.fromPairList(res.getData());
        this.foundIdx = foundIdx;
        hasSkippedLine = res.hasSkippedLine();
        this.requestId = requestId;
        hasNextLine = true;
    }

    public EventSearchResponse(SearchTask.SearchResponse res, long stateVersion, long requestId) {
        this(res, stateVersion, requestId, -1);
    }

    public EventSearchResponse(SearchTask.SearchResponse combRes, LoadNextResponse loadRes, long stateVersion, long requestId, boolean backward) {
        super(combRes.getStatuses(), stateVersion);

        this.requestId = requestId;
        hasSkippedLine = combRes.hasSkippedLine();

        List<RestRecord> beforeOccurrence = RestRecord.fromPairList(combRes.getData());
        List<RestRecord> afterOccurrence = RestRecord.fromPairList(loadRes.getData());

        records = new ArrayList<>(beforeOccurrence.size() + afterOccurrence.size());

        if (backward) {
            records.addAll(afterOccurrence);
            records.addAll(beforeOccurrence);

            foundIdx = afterOccurrence.size();
        } else {
            records.addAll(beforeOccurrence);
            records.addAll(afterOccurrence);

            foundIdx = beforeOccurrence.size() - 1;
        }

        hasNextLine = loadRes.hasNextLine();
    }

    @Override
    public String getName() {
        return "onSearchResponse";
    }
}
