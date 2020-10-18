package com.logviewer.web.dto.events;

import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public class EventResponseAfterFilterChanged extends StatusHolderEvent {

    private final RecordBundle topData;
    private final RecordBundle bottomData;

    public EventResponseAfterFilterChanged(Map<String, Status> statuses, long stateVersion, LoadNextResponse top, LoadNextResponse bottom) {
        super(statuses, stateVersion);

        topData = new RecordBundle(top);
        bottomData = new RecordBundle(bottom);
    }

    @Override
    public String getName() {
        return "onResponseAfterFilterChanged";
    }
}
