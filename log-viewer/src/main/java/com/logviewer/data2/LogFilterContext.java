package com.logviewer.data2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface LogFilterContext {
    LogFormat getLogFormat();

    int findFieldIndexByName(@Nonnull String fieldName);

    @Nullable
    String getFieldValue(@Nonnull Record record, @Nonnull String fieldName);

    @Nonnull
    LogFormat.FieldDescriptor[] getFields();
}
