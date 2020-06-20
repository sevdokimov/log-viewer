package com.logviewer.domain;

import com.logviewer.data2.Position;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.web.session.tasks.SearchPattern;

import java.util.Map;

public class Permalink {

    private String[] paths;

    private SearchPattern searchPattern;
    private boolean hideUnmatched;

    private String savedFiltersName;

    private String filterState;

    private Position offset;

    private Map<String, String> hashes;

    private Position selectedLine;
    private int shiftView;

    private RecordPredicate[] filtersFromFilterPanel;

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

    public String[] getPaths() {
        return paths;
    }

    public void setPaths(String[] paths) {
        this.paths = paths;
    }

    public String getSavedFiltersName() {
        return savedFiltersName;
    }

    public void setSavedFiltersName(String savedFiltersName) {
        this.savedFiltersName = savedFiltersName;
    }

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

    public RecordPredicate[] getFiltersFromFilterPanel() {
        return filtersFromFilterPanel;
    }

    public void setFiltersFromFilterPanel(RecordPredicate[] filtersFromFilterPanel) {
        this.filtersFromFilterPanel = filtersFromFilterPanel;
    }
}
