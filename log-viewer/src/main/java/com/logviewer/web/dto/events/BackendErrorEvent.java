package com.logviewer.web.dto.events;

public class BackendErrorEvent extends BackendEvent {

    private final String stacktrace;

    public BackendErrorEvent(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    @Override
    public String getName() {
        return "backendError";
    }
}
