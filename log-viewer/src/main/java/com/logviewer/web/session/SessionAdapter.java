package com.logviewer.web.session;

import com.logviewer.web.dto.events.BackendEvent;
import org.springframework.lang.NonNull;

public interface SessionAdapter {

    void send(@NonNull BackendEvent event);

}
