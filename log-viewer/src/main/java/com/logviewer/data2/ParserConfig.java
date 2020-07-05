package com.logviewer.data2;

public class ParserConfig {

    public static final int WINDOW_SIZE_BITS = 16;

    public static final int WINDOW_SIZE = 1 << WINDOW_SIZE_BITS;

    public static final int MAX_LINE_LENGTH = 32*1024;

    static {
        assert WINDOW_SIZE >= MAX_LINE_LENGTH * 2;
    }
}
