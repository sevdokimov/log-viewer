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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Import({LvConfigBase.class})
@Configuration
@PropertySource(factory = TypesafePropertySourceFactory.class, value = "")
public class LvStandaloneConfig {

    public static final String LOG_PATHS = "log-paths";

    @Bean
    public LogManager logManager() {
        List<LogDescriptor> descriptors = LogManager.fromHocon(TypesafePropertySourceFactory.getHoconConfig());
        return new LogManager(descriptors);
    }

    @Bean
    public LvFilterPanelStateProvider hoconFilterSet() {
        return new LvHoconFilterPanelStateProvider(TypesafePropertySourceFactory.getHoconConfig());
    }

    @Bean
    public LvUiConfigurer hoconUiConfigurer() {
        Config lvHoconConfig = TypesafePropertySourceFactory.getHoconConfig();

        return () -> {
            if (lvHoconConfig.hasPath("ui-config"))
                return lvHoconConfig.getConfig("ui-config");

            return null;
        };
    }

    @Bean
    public LvPathResolver lvPathFromConfigResolver() {
        Config lvHoconConfig = TypesafePropertySourceFactory.getHoconConfig();

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
