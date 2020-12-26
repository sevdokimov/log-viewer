package com.logviewer.domain;

import com.logviewer.data2.Filter;

import java.util.Arrays;
import java.util.List;

/**
 * See filter-panel-state.service.ts/FilterState typescript class.
 */
public class FilterPanelState {

    private List<String> level;

    private Boolean exceptionsOnly;

    private Filter[] namedFilters;

    private Long startDate;
    private Long endDate;

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

    public FilterPanelState setLevel(List<String> level) {
        this.level = level;
        return this;
    }

    public Boolean getExceptionsOnly() {
        return exceptionsOnly;
    }

    public FilterPanelState setExceptionsOnly(Boolean exceptionsOnly) {
        this.exceptionsOnly = exceptionsOnly;
        return this;
    }

    public Long getStartDate() {
        return startDate;
    }

    public FilterPanelState setStartDate(Long startDate) {
        this.startDate = startDate;
        return this;
    }

    public Long getEndDate() {
        return endDate;
    }

    public FilterPanelState setEndDate(Long endDate) {
        this.endDate = endDate;
        return this;
    }
}
