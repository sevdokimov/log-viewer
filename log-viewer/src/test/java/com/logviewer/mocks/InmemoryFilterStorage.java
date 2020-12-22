package com.logviewer.mocks;

import com.logviewer.api.LvFilterStorage;
import com.logviewer.utils.TestListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class InmemoryFilterStorage implements LvFilterStorage, TestListener {

    private final Map<String, String> map = new HashMap<>();

    @Nullable
    @Override
    public String loadFilterStateByHash(@NonNull String hash) {
        return map.get(hash);
    }

    @Override
    public void saveFilterSet(@NonNull String hash, @NonNull String filters) {
        map.put(hash, filters);
    }

    public Map<String, String> getAllFilters() {
        return map;
    }

    public void clear() {
        map.clear();
    }

    @Override
    public void beforeTest() {
        clear();
    }
}
