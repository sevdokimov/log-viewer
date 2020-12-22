package com.logviewer.mocks;

import com.google.common.hash.Hashing;
import com.logviewer.api.LvPermalinkStorage;
import com.logviewer.domain.Permalink;
import com.logviewer.utils.LvGsonUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class InmemoryPermalinkStorage implements LvPermalinkStorage {

    private final Map<String, Permalink> map = new HashMap<>();

    @Override
    public String save(@Nullable String hash, @NonNull Permalink link) {
        if (hash == null) {
            byte[] json = LvGsonUtils.GSON.toJson(link).getBytes(StandardCharsets.UTF_8);
            hash = Hashing.md5().hashBytes(json).toString().substring(0, 10);
        }

        map.put(hash, link);

        return hash;
    }

    @Override
    public Permalink load(String hash) {
        return map.get(hash);
    }
}
