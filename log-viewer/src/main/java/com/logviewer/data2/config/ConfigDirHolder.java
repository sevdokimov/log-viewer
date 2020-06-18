package com.logviewer.data2.config;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface ConfigDirHolder {
    Path getConfigDir();

    String getProperty(@Nonnull String name);
}
