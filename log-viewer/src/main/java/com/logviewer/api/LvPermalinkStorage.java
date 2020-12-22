package com.logviewer.api;

import com.logviewer.domain.Permalink;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;

public interface LvPermalinkStorage {
    String save(@Nullable String hash, @NonNull Permalink link) throws IOException;

    Permalink load(String hash) throws IOException;
}
