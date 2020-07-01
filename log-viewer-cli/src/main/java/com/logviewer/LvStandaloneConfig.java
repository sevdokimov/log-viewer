package com.logviewer;

import com.logviewer.api.LvFilterPanelStateProvider;
import com.logviewer.api.LvPathResolver;
import com.logviewer.api.LvUiConfigurer;
import com.logviewer.config.LvConfigBase;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.FileFavoriteLogService;
import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.impl.LvHoconFilterPanelStateProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Import({LvConfigBase.class})
@Configuration
public class LvStandaloneConfig {

    public static final String LOG_VIEWER_CONFIG_FILE = "log-viewer.config-file";

    public static final String LOG_PATHS = "log-paths";

    @Bean
    public Config lvHoconConfig(Environment environment, ConfigDirHolder configDirHolder) {
        Path configFile;

        String configPath = environment.getProperty(LOG_VIEWER_CONFIG_FILE);
        if (configPath != null && !configPath.isEmpty()) {
            configFile = Paths.get(configPath);

            if (!Files.exists(configFile))
                throw new RuntimeException("Config file not found: " + LOG_VIEWER_CONFIG_FILE + "=" + configFile);
        } else {
            configFile = configDirHolder.getConfigDir().resolve("config.conf");
            if (!Files.exists(configFile))
                return ConfigFactory.empty();
        }

        return ConfigFactory.parseFile(configFile.toFile()).resolve();
    }

    @Bean
    public LogManager logManager(Config lvHoconConfig) {
        List<LogDescriptor> descriptors = LogManager.fromHocon(lvHoconConfig);
        return new LogManager(descriptors);
    }

    @Bean
    public LvFilterPanelStateProvider hoconFilterSet(Config lvHoconConfig) {
        return new LvHoconFilterPanelStateProvider(lvHoconConfig);
    }

    @Bean
    public LvUiConfigurer hoconUiConfigurer(Config lvHoconConfig) {
        return () -> {
            if (lvHoconConfig.hasPath("ui-config"))
                return lvHoconConfig.getConfig("ui-config");

            return null;
        };
    }

    @Bean
    public LvPathResolver lvPathFromConfigResolver(Config lvHoconConfig) {
        if (lvHoconConfig.hasPath(LOG_PATHS)) {
            return new HoconPathResolver(lvHoconConfig.getObject(LOG_PATHS));
        } else {
            return pathFromHttpParameter -> null;
        }
    }

    @Bean
    public FavoriteLogService lvFavoriteLogService(ConfigDirHolder configDir) {
        return new FileFavoriteLogService(configDir);
    }
}
