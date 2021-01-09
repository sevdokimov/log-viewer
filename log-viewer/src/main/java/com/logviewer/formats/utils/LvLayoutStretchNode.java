package com.logviewer.formats.utils;

import com.logviewer.data2.FieldTypes;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvLayoutStretchNode extends LvLayoutCustomTypeNode {

    protected final boolean removeSpacesBefore;

    protected final int minSize;

    protected int start;
    protected int end;

    public LvLayoutStretchNode(@NonNull String fieldName, @Nullable String fieldType, boolean removeSpacesBefore, int minSize) {
        super(fieldName, fieldType);
        this.removeSpacesBefore = removeSpacesBefore;
        this.minSize = minSize;
        assert minSize >= 0;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean reset(String s, int start, int endStr) {
        this.start = start;

        if (start + minSize > endStr)
            return false;
        
        this.end = start + minSize;

        return true;
    }

    @Override
    public final int parse(String s, int offset, int end) {
        throw new UnsupportedOperationException();
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

    public boolean grow(String s, int targetPosition, int endStr) {
        assert targetPosition <= endStr;

        if (end < targetPosition) {
            end = targetPosition;
        }

        return true;
    }
}
