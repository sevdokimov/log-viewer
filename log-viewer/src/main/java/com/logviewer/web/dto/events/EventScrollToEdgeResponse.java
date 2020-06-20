package com.logviewer.web.dto.events;

import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public class EventScrollToEdgeResponse extends DataHolderEvent {

    private final boolean isScrollToBegin;

    public EventScrollToEdgeResponse(Map<String, Status> statuses, long stateVersion, LoadNextResponse res, boolean isScrollToBegin) {
        super(statuses, stateVersion, res);
        this.isScrollToBegin = isScrollToBegin;
    }

    @Override
    public String getName() {
        return "scrollToEdgeResponse";
    }
}
