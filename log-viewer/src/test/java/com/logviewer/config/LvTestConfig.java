package com.logviewer.config;

import com.logviewer.api.*;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.mocks.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:test.properties")
@Configuration
public class LvTestConfig extends LvConfigBase {

    @Override
    public ConfigDirHolder lvConfigDirHolder(Environment environment) {
        return new TestConfigDirHolder(environment);
    }

    @Bean
    @Override
    public FavoriteLogService lvFavoriteLogService(ConfigDirHolder configDir) {
        return new InmemoryFavoritesService();
    }

    @Bean
    @Override
    public LvFilterStorage lvFilterStorage(ConfigDirHolder configDir) {
        return new InmemoryFilterStorage();
    }

    @Override
    @Bean
    public LvPermalinkStorage lvPermalinkService(ConfigDirHolder configDir) {
        return new InmemoryPermalinkStorage();
    }

    @Bean
    public LvFormatRecognizer testFormatRecognizer() {
        return new TestFormatRecognizer();
    }

    @Bean
    public LvUiConfigurer testUiConfigurer() {
        return new TestUiConfigurer();
    }

    @Bean
    public LvFilterPanelStateProvider testFilterSet() {
        return new TestFilterPanelState();
    }

}
