package com.logviewer.impl;

import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.LogFormat;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class LvFormatRecognizerByPath implements LvFormatRecognizer {

    private final String pathPattern;

    private final LogFormat format;

    private transient volatile Pattern pattern;

    public LvFormatRecognizerByPath(String pathPattern, LogFormat format) {
        this.pathPattern = pathPattern;
        this.format = format;
    }

    @Nullable
    @Override
    public LogFormat getFormat(Path canonicalPath) {
        Pattern pattern = this.pattern;

        if (pattern == null) {
            pattern = Pattern.compile(pathPattern);
            this.pattern = pattern;
        }

        if (!pattern.matcher(canonicalPath.toString()).matches())
            return null;

        return format;
    }
}
