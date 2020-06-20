package com.logviewer.formats.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LvLayoutNumberNode extends LvLayoutCustomTypeNode {

    private final boolean canBeNegative;

    public LvLayoutNumberNode(@Nonnull String fieldName, @Nullable String fieldType) {
        this(fieldName, fieldType, false);
    }

    public LvLayoutNumberNode(@Nonnull String fieldName, @Nullable String fieldType, boolean canBeNegative) {
        super(fieldName, fieldType);
        this.canBeNegative = canBeNegative;
    }

    @Override
    public int parse(String s, int offset, int end) {
        if (offset == end)
            return PARSE_FAILED;

        char c = s.charAt(offset);

        if (c == '-' && canBeNegative) {
            offset++;

            if (offset == end)
                return PARSE_FAILED;

            c = s.charAt(offset);
        }

        if (c < '0' || c > '9')
            return PARSE_FAILED;

        for (int i = offset + 1; i < end; i++) {
            c = s.charAt(i);

            if (c < '0' || c > '9')
                return i;
        }

        return end;
    }

    @Override
    public boolean removeSpacesBefore() {
        return true;
    }

    @Override
    public LvLayoutNumberNode clone() {
        return new LvLayoutNumberNode(getFieldName(), getFieldType(), canBeNegative);
    }

//    @Override
//    public int search(String s, int offset, int end) {
//        for (int i = offset; i < end; i++) {
//            char c = s.charAt(i);
//
//            if (c >= '0' && c <= '9')
//                return i;
//        }
//
//        return -1;
//    }
}
