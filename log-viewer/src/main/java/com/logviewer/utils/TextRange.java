package com.logviewer.utils;

public class TextRange {

    private final int start;

    private final int end;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextRange)) return false;
        TextRange textRange = (TextRange) o;
        return start == textRange.start && end == textRange.end;
    }

    @Override
    public int hashCode() {
        return start * 31 + end;
    }
}

