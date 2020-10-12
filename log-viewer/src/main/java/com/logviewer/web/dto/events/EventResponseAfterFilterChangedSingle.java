package com.logviewer.web.dto.events;

import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public class EventResponseAfterFilterChangedSingle extends DataHolderEvent {

    public EventResponseAfterFilterChangedSingle(Map<String, Status> statuses, long stateVersion, LoadNextResponse res) {
        super(statuses, stateVersion, res);
    }

    @Override
    public String getName() {
        return "onResponseAfterFilterChangedSingle";
    }
}
