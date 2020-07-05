package com.logviewer;

import com.logviewer.data2.LogFormat;

import java.util.List;
import java.util.regex.Pattern;

public class LogDescriptor {

    private final Pattern filePattern;
    private final List<Pattern> subdirPatterns;

    private final LogFormat format;

    public LogDescriptor(Pattern filePattern, List<Pattern> subdirPatterns, LogFormat format) {
        this.filePattern = filePattern;
        this.subdirPatterns = subdirPatterns;
        this.format = format;
    }

    public Pattern getFilePattern() {
        return filePattern;
    }

    public List<Pattern> getSubdirPatterns() {
        return subdirPatterns;
    }

    public LogFormat getFormat() {
        return format;
    }
}
