package com.logviewer.utils;

import java.util.function.Function;

public class TestLogIdGenerator implements Function<String, String> {

    public static final Function<String, String> INSTANCE = new TestLogIdGenerator();

    @Override
    public String apply(String s) {
        int idx = s.lastIndexOf('/');
        return s.substring(idx + 1).replaceAll("-\\d+", "");
    }
}
