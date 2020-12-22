package com.logviewer.web.dto.events;

import com.logviewer.web.dto.RestStatus;
import com.logviewer.web.session.Status;

import java.util.HashMap;
import java.util.Map;

public abstract class StatusHolderEvent extends BackendEvent {

    public final Map<String, RestStatus> statuses;
    public final long stateVersion;

    protected StatusHolderEvent(Map<String, Status> statuses, long stateVersion) {
        this.statuses = new HashMap<>();

        for (Map.Entry<String, Status> entry : statuses.entrySet()) {
            this.statuses.put(entry.getKey(), new RestStatus(entry.getValue()));
        }
        
        this.stateVersion = stateVersion;
    }
}
