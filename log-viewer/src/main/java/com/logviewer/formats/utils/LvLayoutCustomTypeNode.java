package com.logviewer.formats.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class LvLayoutCustomTypeNode implements LvLayoutNode {

    private final String fieldName;

    /**
     * @see com.logviewer.data2.FieldTypes
     */
    private final String fieldType;

    public LvLayoutCustomTypeNode(@Nonnull String fieldName, @Nullable String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @Nonnull
    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public String getFieldType() {
        return fieldType;
    }

    @Override
    public abstract LvLayoutNode clone();

}
