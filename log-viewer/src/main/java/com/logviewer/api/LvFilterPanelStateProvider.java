package com.logviewer.api;

import javax.annotation.Nonnull;
import java.util.Map;

public interface LvFilterPanelStateProvider {

    @Nonnull
    Map<String, String> getFilterSets();

}
