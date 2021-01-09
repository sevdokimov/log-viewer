package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvLayoutNumberNode extends LvLayoutCustomTypeNode {

    private final boolean canBeNegative;

    private final boolean canHaveFraction;

    public LvLayoutNumberNode(@NonNull String fieldName, @Nullable String fieldType) {
        this(fieldName, fieldType, false, false);
    }

    public LvLayoutNumberNode(@NonNull String fieldName, @Nullable String fieldType, boolean canBeNegative) {
        this(fieldName, fieldType, canBeNegative, false);
    }

    public LvLayoutNumberNode(@NonNull String fieldName, @Nullable String fieldType, boolean canBeNegative, boolean canHaveFraction) {
        super(fieldName, fieldType);
        this.canBeNegative = canBeNegative;
        this.canHaveFraction = canHaveFraction;
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

        do {
            offset++;
            if (offset == end)
                return offset;

            c = s.charAt(offset);
        } while (c >= '0' && c <= '9');

        if (canHaveFraction && c == '.') {
            if (offset + 1 < end) {
                c = s.charAt(offset + 1);
                if (c >= '0' && c <= '9') {
                    offset += 2;

                    while (offset < end) {
                        c = s.charAt(offset);
                        if (c >= '0' && c <= '9') {
                            offset++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        return offset;
    }

    @Override
    public boolean removeSpacesBefore() {
        return true;
    }

    @Override
    public LvLayoutNumberNode clone() {
        return new LvLayoutNumberNode(getFieldName(), getFieldType(), canBeNegative, canHaveFraction);
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
