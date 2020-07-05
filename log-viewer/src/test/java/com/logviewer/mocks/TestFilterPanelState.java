package com.logviewer.mocks;

import com.logviewer.api.LvFilterPanelStateProvider;
import com.logviewer.domain.FilterPanelState;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.TestListener;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TestFilterPanelState implements LvFilterPanelStateProvider, TestListener {

    private final Map<String, String> map = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, String> getFilterSets() {
        return map;
    }

    public TestFilterPanelState addFilterSet(@Nonnull String name,
                                             @Nonnull String filters) {
        map.put(name, filters);
        return this;
    }

    public TestFilterPanelState addFilterSet(@Nonnull String name,
                                             @Nonnull FilterPanelState state) {
        addFilterSet(name, LvGsonUtils.GSON.toJson(state));
        return this;
    }

    @Override
    public void beforeTest() {
        map.clear();
    }
}
