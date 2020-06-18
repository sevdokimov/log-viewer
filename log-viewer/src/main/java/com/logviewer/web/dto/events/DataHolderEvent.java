package com.logviewer.web.dto.events;

import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.Map;

public abstract class DataHolderEvent extends StatusHolderEvent {

    public final RecordBundle data;

    public DataHolderEvent(Map<String, Status> statuses, long stateVersion, LoadNextResponse res) {
        super(statuses, stateVersion);

        data = new RecordBundle(res);
    }
}
