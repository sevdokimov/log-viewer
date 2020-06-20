package com.logviewer.mocks;

import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.utils.Utils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestConfigDirHolder implements ConfigDirHolder, DisposableBean {

    private final Environment environment;

    private Path configDir;

    public TestConfigDirHolder(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void destroy() {
        if (configDir != null) {
            try {
                Utils.deleteContent(configDir);
                Files.delete(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            configDir = null;
        }
    }

    @Override
    public synchronized Path getConfigDir() {
        if (configDir == null) {
            try {
                configDir = Files.createTempDirectory("test-cfg-");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configDir;
    }

    @Override
    public String getProperty(@Nonnull String name) {
        return environment.getProperty(name);
    }
}
