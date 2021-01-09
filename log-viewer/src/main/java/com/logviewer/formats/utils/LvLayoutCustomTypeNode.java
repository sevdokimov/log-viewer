package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public abstract class LvLayoutCustomTypeNode implements LvLayoutNode {

    private final String fieldName;

    /**
     * @see com.logviewer.data2.FieldTypes
     */
    private final String fieldType;

    public LvLayoutCustomTypeNode(@NonNull String fieldName, @Nullable String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @NonNull
    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public String getFieldType() {
        return fieldType;
    }

    @Override
    public abstract LvLayoutNode clone();

    @Override
    public String toString() {
        return fieldName;
    }
}
