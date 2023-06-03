package com.logviewer.web.dto.events;

import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;
import com.logviewer.web.session.tasks.SearchTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public EventSearchResponse(SearchTask.SearchResponse combRes, LoadNextResponse loadRes, long stateVersion, long requestId, boolean backward) {
        super(combineStatuses(combRes, loadRes), stateVersion);

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
    }

    private static Map<String, Status> combineStatuses(SearchTask.SearchResponse combRes, LoadNextResponse loadRes) {
        Map<String, Status> res = new HashMap<>();

        for (Map.Entry<String, Status> entry : combRes.getStatuses().entrySet()) {
            if (entry.getValue().getError() != null) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Status> entry : loadRes.getStatuses().entrySet()) {
            res.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return res;
    }

    @Override
    public String getName() {
        return "onSearchResponse";
    }
}
