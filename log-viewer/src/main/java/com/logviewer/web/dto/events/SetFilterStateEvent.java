package com.logviewer.web.dto.events;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class SetFilterStateEvent extends BackendEvent {

    private final String urlParamValue;
    private final String filterState;

    public SetFilterStateEvent(@Nullable String urlParamValue, @NonNull String filterState) {
        this.urlParamValue = urlParamValue;
        this.filterState = filterState;
    }

    @Override
    public String getName() {
        return "onSetFilterState";
    }
}
