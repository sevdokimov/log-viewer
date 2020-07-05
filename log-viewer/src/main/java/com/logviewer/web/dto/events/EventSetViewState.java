package com.logviewer.web.dto.events;

import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.LogView;
import com.logviewer.web.dto.RestLog;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventSetViewState extends BackendEvent {

    private List<RestLog> logs;

    private String uiConfig;

    private boolean inFavorites;
    private boolean favEditable;

    private boolean initByPermalink;

    private final Map<String, String> globalSavedFilters;

    private final String filterState;

    public EventSetViewState(LogView[] logs, Config uiConfig, FavoriteLogService favoriteLogService,
                             Map<String, String> globalSavedFilters, String filterState,
                             boolean initByPermalink) {
        this.logs = Stream.of(logs).map(RestLog::new).collect(Collectors.toList());

        this.uiConfig = uiConfig.root().render(ConfigRenderOptions.concise());
        favEditable = favoriteLogService.isEditable();
        this.globalSavedFilters = globalSavedFilters;
        this.filterState = filterState;

        this.initByPermalink = initByPermalink;
    }

    public String getUiConfig() {
        return uiConfig;
    }

    @Override
    public String getName() {
        return "setViewState";
    }
}
