package com.logviewer.web.dto.events;

public class EventBrokenLink extends BackendEvent {
    @Override
    public String getName() {
        return "onBrokenLink";
    }
}
