package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LvLayoutRegexNode extends LvLayoutCustomTypeNode implements LvLayoutNodeSearchable {

    private final Pattern pattern;

    private Matcher matcher;

    public LvLayoutRegexNode(@NonNull String fieldName, @Nullable String fieldType, String pattern) {
        this(fieldName, fieldType, Pattern.compile(pattern));
    }

    public LvLayoutRegexNode(@NonNull String fieldName, @Nullable String fieldType, Pattern pattern) {
        super(fieldName, fieldType);
        
        this.pattern = pattern;
    }

    private Matcher getMatcher(String s) {
        if (matcher == null) {
            matcher = pattern.matcher(s);
        } else {
            matcher.reset(s);
        }

        return matcher;
    }

    @Override
    public int parse(String s, int offset, int end) {
        Matcher matcher = getMatcher(s);

        matcher.region(offset, end);

        if (matcher.lookingAt()) {
            return matcher.end();
        }

        return PARSE_FAILED;
    }

    @Override
    public int search(String s, int offset, int end) {
        Matcher matcher = getMatcher(s);

        matcher.region(offset, end);

        if (matcher.find()) {
            return matcher.start();
        }

        return PARSE_FAILED;
    }

    @Override
    public LvLayoutRegexNode clone() {
        return new LvLayoutRegexNode(getFieldName(), getFieldType(), pattern);
    }
}
