package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvLayoutHexNode extends LvLayoutCustomTypeNode {

    private final int minLength;

    public LvLayoutHexNode(@NonNull String fieldName, @Nullable String fieldType) {
        this(fieldName, fieldType, 0);
    }

    public LvLayoutHexNode(@NonNull String fieldName, String fieldType, int minLength) {
        super(fieldName, fieldType);
        this.minLength = minLength;
    }

    @Override
    public int parse(String s, int offset, int end) {
        int i = offset;

        while (i < end) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                i++;
            } else {
                break;
            }
        }

        if (i - offset < minLength)
            return PARSE_FAILED;

        return i;
    }

    @Override
    public boolean removeSpacesBefore() {
        return true;
    }

    @Override
    public LvLayoutHexNode clone() {
        return new LvLayoutHexNode(getFieldName(), getFieldType(), minLength);
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
