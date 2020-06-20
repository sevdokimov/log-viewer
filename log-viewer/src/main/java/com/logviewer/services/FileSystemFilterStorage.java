package com.logviewer.services;

import com.logviewer.api.LvFilterStorage;
import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.utils.PersistentMap;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FileSystemFilterStorage implements LvFilterStorage, InitializingBean {

    private final ConfigDirHolder configDir;

    private PersistentMap persistentMap;

    public FileSystemFilterStorage(ConfigDirHolder configDir) {
        this.configDir = configDir;
    }

    @Override
    public void afterPropertiesSet() {
        String maxSizeStr = configDir.getProperty("filter.state.max.size");
        int maxSize = maxSizeStr == null ? 2 * 1024*1024 : Integer.parseInt(maxSizeStr);

        persistentMap = new PersistentMap(configDir.getConfigDir().resolve("filters_state.data"), maxSize);
    }

    @Nullable
    @Override
    public String loadFilterStateByHash(@Nonnull String hash) {
        return persistentMap.get(hash);
    }

    @Override
    public void saveFilterSet(@Nonnull String hash, @Nonnull String filters) {
        persistentMap.put(hash, filters);
    }
}
