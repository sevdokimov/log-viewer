package com.logviewer.api;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface LvFilterStorage {

    @Nullable
    String loadFilterStateByHash(@NonNull String hash);

    void saveFilterSet(@NonNull String hash, @NonNull String filters);

}
