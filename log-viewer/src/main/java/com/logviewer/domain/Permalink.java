package com.logviewer.domain;

import com.logviewer.data2.Position;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.web.dto.LogList;
import com.logviewer.web.session.tasks.SearchPattern;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

public class Permalink {

    private Map logListQueryParams;

    private LogList logList;

    private SearchPattern searchPattern;
    private boolean hideUnmatched;

    private String savedFiltersName;

    private String filterState;

    private String filterStateUrlParam;

    private Position offset;

    private Map<String, String> hashes;

    private Position selectedLine;
    private int shiftView;

    private RecordPredicate[] filterPanelFilters;

    public SearchPattern getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(SearchPattern searchPattern) {
        this.searchPattern = searchPattern;
    }

    public boolean isHideUnmatched() {
        return hideUnmatched;
    }

    public void setHideUnmatched(boolean hideUnmatched) {
        this.hideUnmatched = hideUnmatched;
    }

    public Map getLogListQueryParams() {
        return logListQueryParams;
    }

    public Permalink setLogListQueryParams(Map logListQueryParams) {
        this.logListQueryParams = logListQueryParams;
        return this;
    }

    public LogList getLogList() {
        return logList;
    }

    public Permalink setLogList(LogList logList) {
        this.logList = logList;
        return this;
    }

    public String getSavedFiltersName() {
        return savedFiltersName;
    }

    public void setSavedFiltersName(String savedFiltersName) {
        this.savedFiltersName = savedFiltersName;
    }

    @NonNull
    public String getFilterState() {
        return filterState;
    }

    public void setFilterState(String filterState) {
        this.filterState = filterState;
    }

    public Position getOffset() {
        return offset;
    }

    public void setOffset(Position offset) {
        this.offset = offset;
    }

    public Map<String, String> getHashes() {
        return hashes;
    }

    public void setHashes(Map<String, String> hashes) {
        this.hashes = hashes;
    }

    public Position getSelectedLine() {
        return selectedLine;
    }

    public void setSelectedLine(Position selectedLine) {
        this.selectedLine = selectedLine;
    }

    public int getShiftView() {
        return shiftView;
    }

    public void setShiftView(int shiftView) {
        this.shiftView = shiftView;
    }

    public RecordPredicate[] getFilterPanelFilters() {
        return filterPanelFilters;
    }

    public void setFilterPanelFilters(RecordPredicate[] filterPanelFilters) {
        this.filterPanelFilters = filterPanelFilters;
    }

    @Nullable
    public String getFilterStateUrlParam() {
        return filterStateUrlParam;
    }

    public void setFilterStateUrlParam(String filterStateUrlParam) {
        this.filterStateUrlParam = filterStateUrlParam;
    }
}
