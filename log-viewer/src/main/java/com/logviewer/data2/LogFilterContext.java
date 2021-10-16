package com.logviewer.data2;

import org.springframework.lang.NonNull;

import java.util.function.Function;

public interface LogFilterContext {
    LogFormat getLogFormat();

    @NonNull
    LogFormat.FieldDescriptor[] getFields();

    <T> T getProperty(String name, Function<String, T> factory);
}
