package com.logviewer.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface LvFilterStorage {

    @Nullable
    String loadFilterStateByHash(@Nonnull String hash);

    void saveFilterSet(@Nonnull String hash, @Nonnull String filters);

}
