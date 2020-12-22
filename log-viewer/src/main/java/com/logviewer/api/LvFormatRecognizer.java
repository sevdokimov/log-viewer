package com.logviewer.api;

import com.logviewer.data2.LogFormat;
import org.springframework.lang.Nullable;

import java.nio.file.Path;

public interface LvFormatRecognizer {

    @Nullable
    LogFormat getFormat(Path canonicalPath);

}
