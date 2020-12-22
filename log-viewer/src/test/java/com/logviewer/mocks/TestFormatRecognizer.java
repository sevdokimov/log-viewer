package com.logviewer.mocks;

import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.LogFormat;
import com.logviewer.utils.TestListener;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestFormatRecognizer implements LvFormatRecognizer, TestListener {

    private LogFormat defaultFormat;

    private final Map<String, LogFormat> map = new LinkedHashMap<>();

    @Nullable
    @Override
    public LogFormat getFormat(Path canonicalPath) {
        return map.getOrDefault(canonicalPath.toString(), defaultFormat);
    }

    public LogFormat getDefaultFormat() {
        return defaultFormat;
    }

    public void setFormat(LogFormat defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    public void setFormat(String path, LogFormat format) {
        map.put(path, format);
    }

    @Override
    public void beforeTest() {
        defaultFormat = null;
        map.clear();
    }
}
