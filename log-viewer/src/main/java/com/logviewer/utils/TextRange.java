package com.logviewer.utils;

public class TextRange {

    private int start;

    private int end;

    public TextRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ')';
    }
}

