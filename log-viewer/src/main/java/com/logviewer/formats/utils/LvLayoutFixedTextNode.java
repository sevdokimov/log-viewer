package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public class LvLayoutFixedTextNode extends LvLayoutCustomTypeNode implements LvLayoutNode {

    private final String[] values;

    public LvLayoutFixedTextNode(@NonNull String fieldName, @Nullable String fieldType, String ... values) {
        super(fieldName, fieldType);

        this.values = values.clone();
        Arrays.sort(this.values, Comparator.comparingInt(s -> -s.length()));
    }

    @Override
    public int parse(String s, int offset, int end) {
        for (String value : values) {
            if (offset + value.length() <= end && s.startsWith(value, offset))
                return offset + value.length();
        }

        return PARSE_FAILED;
    }

    @Override
    public LvLayoutFixedTextNode clone() {
        return new LvLayoutFixedTextNode(getFieldName(), getFieldType(), values);
    }
}
