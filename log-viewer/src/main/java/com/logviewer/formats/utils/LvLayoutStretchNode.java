package com.logviewer.formats.utils;

import com.logviewer.data2.FieldTypes;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvLayoutStretchNode extends LvLayoutCustomTypeNode {

    private final boolean removeSpacesBefore;

    private final int minSize;

    public LvLayoutStretchNode(@NonNull String fieldName, @Nullable String fieldType, boolean removeSpacesBefore, int minSize) {
        super(fieldName, fieldType);
        this.removeSpacesBefore = removeSpacesBefore;
        this.minSize = minSize;
        assert minSize >= 0;
    }

    @Override
    public int parse(String s, int offset, int end) {
        if (offset + minSize > end)
            return PARSE_FAILED;

        return -1 - minSize;
    }

    @Override
    public boolean removeSpacesBefore() {
        return removeSpacesBefore;
    }

    public int getMinSize() {
        return minSize;
    }

    @Override
    public LvLayoutStretchNode clone() {
        return new LvLayoutStretchNode(getFieldName(), getFieldType(), removeSpacesBefore, minSize);
    }

    public static LvLayoutStretchNode threadNode() {
        return new LvLayoutStretchNode("thread", FieldTypes.THREAD, true, 1);
    }

    public static LvLayoutStretchNode messageNode() {
        return new LvLayoutStretchNode("msg", FieldTypes.MESSAGE, true, 0);
    }
}
