package com.logviewer.web.dto.events;

import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public class EventResponseAfterFilterScrollDown extends DataHolderEvent {

    public EventResponseAfterFilterScrollDown(Map<String, Status> statuses, long stateVersion, LoadNextResponse res) {
        super(statuses, stateVersion, res);
    }

    @Override
    public String getName() {
        return "onResponseAfterFilterChangedScrollDown";
    }
}
