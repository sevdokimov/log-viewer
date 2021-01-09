package com.logviewer.logLibs.nginx;

import com.logviewer.formats.utils.LvLayoutNumberNode;
import org.springframework.lang.NonNull;

public class NginxOptionalNumber extends LvLayoutNumberNode {

    public NginxOptionalNumber(@NonNull String fieldName, String fieldType) {
        super(fieldName, fieldType, false);
    }

    @Override
    public int parse(String s, int offset, int end) {
        if (offset == end)
            return PARSE_FAILED;

        if (s.charAt(offset) == '-') { // skipped field
            return offset + 1;
        }

        return super.parse(s, offset, end);
    }

    @Override
    public LvLayoutNumberNode clone() {
        return new NginxOptionalNumber(getFieldName(), getFieldType());
    }
}
