package com.logviewer.formats.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LvLayoutFixedTextNode extends LvLayoutCustomTypeNode implements LvLayoutNode {

    private final String[] values;

    public LvLayoutFixedTextNode(@Nonnull String fieldName, @Nullable String fieldType, String ... values) {
        super(fieldName, fieldType);
        this.values = values;
    }

    @Override
    public int parse(String s, int offset, int end) {
        for (String value : values) {
            if (s.startsWith(value, offset))
                return offset + value.length();
        }

        return PARSE_FAILED;
    }

    @Override
    public boolean removeSpacesBefore() {
        return true;
    }

    @Override
    public LvLayoutFixedTextNode clone() {
        return new LvLayoutFixedTextNode(getFieldName(), getFieldType(), values);
    }
}
