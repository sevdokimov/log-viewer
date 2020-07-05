package com.logviewer.mocks;

import com.logviewer.api.LvUiConfigurer;
import com.logviewer.utils.TestListener;
import com.typesafe.config.Config;

public class TestUiConfigurer implements LvUiConfigurer, TestListener {

    private Config config;

    @Override
    public Config getUiConfig() {
        return config;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public void beforeTest() {
        config = null;
    }
}
