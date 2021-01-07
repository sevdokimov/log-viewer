package com.logviewer.web.dto;

import org.springframework.lang.Nullable;

public class LogList {

    private String[] pathsInLegacyFormat;
    private String[] files;
    private String[] ssh;
    private String[] bookmarks;

    @Nullable
    public String[] getPathsInLegacyFormat() {
        return pathsInLegacyFormat;
    }

    public LogList setPathsInLegacyFormat(@Nullable String[] pathsInLegacyFormat) {
        this.pathsInLegacyFormat = pathsInLegacyFormat;
        return this;
    }

    public String[] getFiles() {
        return files;
    }

    public LogList setFiles(String[] files) {
        this.files = files;
        return this;
    }

    public String[] getSsh() {
        return ssh;
    }

    public LogList setSsh(String[] ssh) {
        this.ssh = ssh;
        return this;
    }

    @Nullable
    public String[] getBookmarks() {
        return bookmarks;
    }

    public LogList setBookmarks(@Nullable String ... bookmarks) {
        this.bookmarks = bookmarks;
        return this;
    }

    public static LogList of(String ... files) {
        LogList res = new LogList();

        res.pathsInLegacyFormat = files;

        return res;
    }
}
