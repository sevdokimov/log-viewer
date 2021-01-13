package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.function.Function;

public interface LogFilterContext {
    LogFormat getLogFormat();

    int findFieldIndexByName(@NonNull String fieldName);

    @Nullable
    String getFieldValue(@NonNull Record record, @NonNull String fieldName);

    @NonNull
    LogFormat.FieldDescriptor[] getFields();

    <T> T getProperty(String name, Function<String, T> factory);
}
