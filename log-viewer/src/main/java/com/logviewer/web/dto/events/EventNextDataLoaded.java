package com.logviewer.web.dto.events;

import com.logviewer.data2.Position;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public class EventNextDataLoaded extends DataHolderEvent {

    private final Position start;
    private final boolean backward;

    public EventNextDataLoaded(Map<String, Status> statuses, long stateVersion, LoadNextResponse res,
                               Position start, boolean backward) {
        super(statuses, stateVersion, res);
        this.start = start;
        this.backward = backward;
    }

    @Override
    public String getName() {
        return "nextDataLoaded";
    }
}
