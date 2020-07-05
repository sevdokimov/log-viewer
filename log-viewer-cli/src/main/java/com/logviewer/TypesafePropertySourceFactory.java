package com.logviewer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TypesafePropertySourceFactory implements PropertySourceFactory {

    public static final String LOG_VIEWER_CONFIG_FILE = "log-viewer.config-file";

    private static volatile Config configCache;

    @Override
    public PropertySource<?> createPropertySource(String s, EncodedResource encodedResource) {
        return new TypesafeConfigPropertySource("HOCON", getHoconConfig());
    }

    public static Config getHoconConfig() {
        Config res = configCache;
        if (res == null) {
            String configPath = System.getProperty(LOG_VIEWER_CONFIG_FILE);
            if (configPath == null || configPath.isEmpty()) {
                throw new RuntimeException("Config file is not specified, specify '" + LOG_VIEWER_CONFIG_FILE + "' system property");
            }

            Path configFile = Paths.get(configPath);

            if (!Files.exists(configFile))
                throw new RuntimeException("Config file not found: " + LOG_VIEWER_CONFIG_FILE + "=" + configFile);

            res = ConfigFactory.parseFile(configFile.toFile()).resolve();

            configCache = res;
        }

        return res;
    }
}
