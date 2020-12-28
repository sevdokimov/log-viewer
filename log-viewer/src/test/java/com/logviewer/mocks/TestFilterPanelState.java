package com.logviewer.mocks;

import com.logviewer.api.LvFilterPanelStateProvider;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.TestListener;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

public class TestFilterPanelState implements LvFilterPanelStateProvider, TestListener {

    private final Map<String, String> map = new HashMap<>();

    @NonNull
    @Override
    public Map<String, String> getFilterSets() {
        return map;
    }

    public TestFilterPanelState addFilterSet(@NonNull String name,
                                             @NonNull String filters) {
        map.put(name, filters);
        return this;
    }

    public TestFilterPanelState addFilterSet(@NonNull String name,
                                             @NonNull FilterPanelState state) {
        addFilterSet(name, LvGsonUtils.GSON.toJson(state));
        return this;
    }

    @Override
    public void beforeTest() {
        map.clear();
    }
}
