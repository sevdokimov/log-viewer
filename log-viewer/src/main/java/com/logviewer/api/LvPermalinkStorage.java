package com.logviewer.api;

import com.logviewer.domain.Permalink;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public interface LvPermalinkStorage {
    String save(@Nullable String hash, @Nonnull Permalink link) throws IOException;

    Permalink load(String hash) throws IOException;
}
