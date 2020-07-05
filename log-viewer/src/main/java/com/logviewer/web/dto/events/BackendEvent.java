package com.logviewer.web.dto.events;

public abstract class BackendEvent {

    private final String name;

    protected BackendEvent() {
        this.name = getName();
    }

    public abstract String getName();

}
