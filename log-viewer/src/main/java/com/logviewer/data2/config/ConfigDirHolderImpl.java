package com.logviewer.data2.config;

import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigDirHolderImpl implements ConfigDirHolder {

    public static final String CONFIG_DIR_PROPERTY = "log-viewer.config-dir";

    private final Path configDir;

    private final Environment environment;

    public ConfigDirHolderImpl(Environment environment) {
        this.environment = environment;

        String configDir = environment.getProperty(CONFIG_DIR_PROPERTY);

        if (configDir == null)
            configDir = System.getProperty("user.home") + "/.log-viewer";

        this.configDir = Paths.get(configDir);

        if (!Files.isDirectory(this.configDir)) {
            try {
                Files.createDirectory(this.configDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create config directory: " + this.configDir, e);
            }
        }
    }

    @Override
    public Path getConfigDir() {
        return configDir;
    }

    @Override
    public String getProperty(@NonNull String name) {
        return environment.getProperty(name);
    }
}
