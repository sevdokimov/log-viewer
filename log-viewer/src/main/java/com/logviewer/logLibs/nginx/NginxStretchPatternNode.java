package com.logviewer.logLibs.nginx;

import org.springframework.lang.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NginxStretchPatternNode extends NginxStretchNode {

    private final Pattern pattern;

    public NginxStretchPatternNode(@NonNull String fieldName, String fieldType, boolean removeSpacesBefore, Pattern pattern) {
        super(fieldName, fieldType, removeSpacesBefore, 0);
        this.pattern = pattern;
    }

    @Override
    public boolean reset(String s, int start, int endStr) {
        Matcher matcher = pattern.matcher(s);
        matcher.region(start, endStr);

        if (!matcher.lookingAt())
            return false;

        this.start = start;
        this.end = matcher.end();

        return true;
    }

    @Override
    public NginxStretchPatternNode clone() {
        return new NginxStretchPatternNode(getFieldName(), getFieldType(), removeSpacesBefore, pattern);
    }
}
