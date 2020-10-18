package com.logviewer.web.dto.events;

import com.logviewer.data2.FileAttributes;

import java.util.Map;

public class EventLogChanged extends BackendEvent {

    private final Map<String, FileAttributes> changedLogs;

    public EventLogChanged(Map<String, FileAttributes> changedLogs) {
        this.changedLogs = changedLogs;
    }

    @Override
    public String getName() {
        return "onLogChanged";
    }
}
