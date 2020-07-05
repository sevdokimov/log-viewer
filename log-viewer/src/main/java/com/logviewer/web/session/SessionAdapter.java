package com.logviewer.web.session;

import com.logviewer.web.dto.events.BackendEvent;

import javax.annotation.Nonnull;

public interface SessionAdapter {

    void send(@Nonnull BackendEvent event);

}
