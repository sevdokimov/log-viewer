package com.logviewer.data2.config;

import org.springframework.lang.NonNull;

import java.nio.file.Path;

public interface ConfigDirHolder {
    Path getConfigDir();

    String getProperty(@NonNull String name);
}
