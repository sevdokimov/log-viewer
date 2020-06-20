package com.logviewer.web.dto.events;

import com.google.common.collect.Maps;
import com.logviewer.web.dto.RestStatus;
import com.logviewer.web.session.Status;

import java.util.Map;

public abstract class StatusHolderEvent extends BackendEvent {

    public final Map<String, RestStatus> statuses;
    public final long stateVersion;

    protected StatusHolderEvent(Map<String, Status> statuses, long stateVersion) {
        this.statuses = Maps.transformValues(statuses, RestStatus::new);
        this.stateVersion = stateVersion;
    }
}
