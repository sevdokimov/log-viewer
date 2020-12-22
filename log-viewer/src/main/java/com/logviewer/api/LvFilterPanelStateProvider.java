package com.logviewer.api;

import org.springframework.lang.NonNull;

import java.util.Map;

public interface LvFilterPanelStateProvider {

    @NonNull
    Map<String, String> getFilterSets();

}
