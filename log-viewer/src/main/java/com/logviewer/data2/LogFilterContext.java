package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface LogFilterContext {
    LogFormat getLogFormat();

    int findFieldIndexByName(@NonNull String fieldName);

    @Nullable
    String getFieldValue(@NonNull Record record, @NonNull String fieldName);

    @NonNull
    LogFormat.FieldDescriptor[] getFields();
}
