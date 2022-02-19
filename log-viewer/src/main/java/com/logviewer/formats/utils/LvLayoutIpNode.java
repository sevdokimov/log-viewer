package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvLayoutIpNode extends LvLayoutCustomTypeNode {

    public LvLayoutIpNode(@NonNull String fieldName, @Nullable String fieldType) {
        super(fieldName, fieldType);
    }

    private int readNumber(String s, int offset, int end) {
        if (offset == end)
            return PARSE_FAILED;

        char c1 = s.charAt(offset);
        if (c1 < '0' || c1 > '9')
            return PARSE_FAILED;

        offset++;

        if (offset == end)
            return offset;

        char c2 = s.charAt(offset);

        if (c2 < '0' || c2 > '9')
            return offset;

        offset++;

        if (offset == end)
            return offset;

        char c3 = s.charAt(offset);

        if (c3 < '0' || c3 > '9')
            return offset;

        return offset + 1;
    }

    @Override
    public int parse(String s, int offset, int end) {
        for (int i = 0; i < 3; i++) {
            offset = readNumber(s, offset, end);
            if (offset == PARSE_FAILED)
                return PARSE_FAILED;

            if (offset == end || s.charAt(offset) != '.')
                return PARSE_FAILED;

            offset++;
        }

        int res = readNumber(s, offset, end);
        if (res == PARSE_FAILED)
            return PARSE_FAILED;

        if (res < end) {
            char a = s.charAt(res);
            if (a >= '0' && a <= '9')
                return PARSE_FAILED;
        }

        return res;
    }

    @Override
    public LvLayoutIpNode clone() {
        return new LvLayoutIpNode(getFieldName(), getFieldType());
    }
}
