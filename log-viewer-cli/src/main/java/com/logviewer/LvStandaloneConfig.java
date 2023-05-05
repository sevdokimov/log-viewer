package com.logviewer;

import com.logviewer.api.*;
import com.logviewer.config.LvConfigBase;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.FileFavoriteLogService;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.impl.LvHoconFilterPanelStateProvider;
import com.logviewer.impl.LvPatternFormatRecognizer;
import com.logviewer.data2.LogLevels;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.services.PathPattern;
import com.logviewer.utils.Pair;
import com.typesafe.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.stream.Collectors;

@Import({LvConfigBase.class})
@Configuration
@PropertySource(factory = TypesafePropertySourceFactory.class, value = "")
public class LvStandaloneConfig {

    public static final String LOG_PATHS = "log-paths";

    @Bean
    public LvFileAccessManagerImpl logManager() {
        Config config = TypesafePropertySourceFactory.getHoconConfig();
        List<Pair<PathPattern, LogFormat>> pairs = LvPatternFormatRecognizer.fromHocon(config);
        return new LvFileAccessManagerImpl(pairs.stream().map(Pair::getFirst).collect(Collectors.toList()));
    }

    @Bean
    public LvFormatRecognizer logFormatRecognizer() {
        Config config = TypesafePropertySourceFactory.getHoconConfig();
        List<Pair<PathPattern, LogFormat>> pairs = LvPatternFormatRecognizer.fromHocon(config);
        return new LvPatternFormatRecognizer(pairs);
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
    public LogLevels hoconLogLevels() {
        return new LogLevels(TypesafePropertySourceFactory.getHoconConfig());
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
