package com.logviewer.utils;

import java.util.Arrays;
import java.util.List;

/**
 * See filter-panel-state.service.ts/FilterState typescript class.
 */
public class FilterPanelState {

    private List<String> level;

    private Boolean exceptionsOnly;

    private GroovyFilter[] groovyFilters;

    private DateFilter date;
    private ThreadFilter thread;

    public List<String> getLevel() {
        return level;
    }

    public FilterPanelState setLevel(String ... levels) {
        this.level = Arrays.asList(levels);
        return this;
    }

    public FilterPanelState setLevel(List<String> level) {
        this.level = level;
        return this;
    }

    public GroovyFilter[] getGroovyFilters() {
        return groovyFilters;
    }

    public FilterPanelState groovyFilter(GroovyFilter ... filters) {
        this.groovyFilters = filters;
        return this;
    }

    public Boolean getExceptionsOnly() {
        return exceptionsOnly;
    }

    public FilterPanelState setExceptionsOnly(Boolean exceptionsOnly) {
        this.exceptionsOnly = exceptionsOnly;
        return this;
    }

    public FilterPanelState setDate(DateFilter date) {
        this.date = date;
        return this;
    }

    public FilterPanelState setStartDate(Long startDate) {
        if (date == null)
            date = new DateFilter();

        date.startDate = startDate;
        return this;
    }

    public FilterPanelState setEndDate(Long endDate) {
        if (date == null)
            date = new DateFilter();

        date.endDate = endDate;
        return this;
    }

    public ThreadFilter getThread() {
        return thread;
    }

    public FilterPanelState setThread(ThreadFilter thread) {
        this.thread = thread;
        return this;
    }

    public FilterPanelState excludeThreads(String ... threadNames) {
        if (thread == null)
            thread = new ThreadFilter();

        thread.excludes = threadNames;

        return this;
    }

    public FilterPanelState includeThreads(String ... threadNames) {
        if (thread == null)
            thread = new ThreadFilter();

        thread.includes = threadNames;

        return this;
    }

    public static class ThreadFilter {
        private String[] excludes;

        private String[] includes;
    }

    public static class GroovyFilter {
        private String id;

        private String name;

        private String script;

        public GroovyFilter(String id, String name, String script) {
            this.id = id;
            this.name = name;
            this.script = script;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getScript() {
            return script;
        }
    }

    public static class DateFilter {
        private Long startDate;
        private Long endDate;
    }
}
