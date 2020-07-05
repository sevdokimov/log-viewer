package com.logviewer.domain;

import com.logviewer.data2.Filter;

import java.util.Arrays;
import java.util.List;

/**
 * See filter-panel-state.service.ts/FilterState typescript class.
 */
public class FilterPanelState {

    private List<String> level;

    private Filter[] namedFilters;

    public List<String> getLevel() {
        return level;
    }

    public FilterPanelState setLevel(String ... levels) {
        this.level = Arrays.asList(levels);
        return this;
    }

    public Filter[] getNamedFilters() {
        return namedFilters;
    }

    public FilterPanelState setNamedFilters(Filter ... namedFilters) {
        this.namedFilters = namedFilters;
        return this;
    }
}
