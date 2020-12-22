package com.logviewer.impl;

import com.logviewer.api.LvFilterPanelStateProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LvHoconFilterPanelStateProvider implements LvFilterPanelStateProvider {

    public static final String CONFIG_PATH = "filters";

    private final Config lvHoconConfig;

    private volatile Map<String, String> filters;

    public LvHoconFilterPanelStateProvider(@NonNull Config lvHoconConfig) {
        this.lvHoconConfig = lvHoconConfig;
    }

    @NonNull
    @Override
    public Map<String, String> getFilterSets() {
        Map<String, String> res = this.filters;

        if (res == null) {
            if (lvHoconConfig.hasPath(CONFIG_PATH)) {
                res = new LinkedHashMap<>();

                for (Map.Entry<String, ConfigValue> entry : lvHoconConfig.getObject(CONFIG_PATH).entrySet()) {
                    String filterSetName = entry.getKey();

                    String json = entry.getValue().render(ConfigRenderOptions.concise());
                    res.put(filterSetName, json);
                }
            } else {
                res = Collections.emptyMap();
            }

            filters = res;
        }

        return res;
    }
}
