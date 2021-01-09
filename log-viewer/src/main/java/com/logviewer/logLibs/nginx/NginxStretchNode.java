package com.logviewer.logLibs.nginx;

import com.logviewer.formats.utils.LvLayoutStretchNode;
import org.springframework.lang.NonNull;

public class NginxStretchNode extends LvLayoutStretchNode {

    public NginxStretchNode(@NonNull String fieldName, String fieldType, boolean removeSpacesBefore) {
        super(fieldName, fieldType, removeSpacesBefore, 0);
    }

    public NginxStretchNode(@NonNull String fieldName, String fieldType, boolean removeSpacesBefore, int minSize) {
        super(fieldName, fieldType, removeSpacesBefore, minSize);
    }

    private static boolean isHex(char a) {
        return (a >= '0' && a <= '9') || (a >= 'a' && a <= 'f') || (a >= 'A' && a <= 'F');
    }

    protected boolean isAcceptableSymbol(char a) {
        return true;
    }

    private boolean doGrow(String s, int endStr) {
        if (end == endStr)
            return false;

        char a = s.charAt(end++);

        if (a != '\\')
            return isAcceptableSymbol(a);

        if (end == endStr)
            return true;

        a = s.charAt(end++);
        if (a == 'x') { // escaping '\x55'
            if (endStr - end < 2 || !isHex(s.charAt(end)) || !isHex(s.charAt(end + 1)))
                return true;

            end += 2;
            return true;
        }

        if (a == 'u') {
            if (endStr - end < 4 || !isHex(s.charAt(end)) || !isHex(s.charAt(end + 1))
                    || !isHex(s.charAt(end + 2)) || !isHex(s.charAt(end + 3))) {
                return true;
            }

            end += 4;
            return true;
        }

        return true;
    }

    @Override
    public boolean reset(String s, int start, int endStr) {
        this.start = start;
        this.end = start;

        while (minSize > end - start) {
            if (!doGrow(s, endStr))
                return false;
        }

        return true;
    }

    @Override
    public boolean grow(String s, int targetPosition, int endStr) {
        while (end < targetPosition) {
            if (!doGrow(s, endStr))
                return false;
        }

        return true;
    }

    @Override
    public NginxStretchNode clone() {
        return new NginxStretchNode(getFieldName(), getFieldType(), removeSpacesBefore, minSize);
    }
}
