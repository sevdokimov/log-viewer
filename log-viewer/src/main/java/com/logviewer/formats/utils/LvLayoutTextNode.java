package com.logviewer.formats.utils;

import javax.annotation.Nonnull;

public class LvLayoutTextNode implements LvLayoutNode, LvLayoutNodeSearchable, Cloneable {

    private final String txt;

    private final String trimmedStartTxt;

    private final int prefixSpaces;

    public LvLayoutTextNode(@Nonnull String txt) {
        assert txt.length() > 0;

        this.txt = txt;

        int prefixSpaces = 0;
        while (prefixSpaces < txt.length() && txt.charAt(prefixSpaces) == ' ') {
            prefixSpaces++;
        }

        this.prefixSpaces = prefixSpaces;

        this.trimmedStartTxt = txt.substring(prefixSpaces);
    }

    @Override
    public int parse(String s, int offset, int end) {
        int nonSpaceIdx = offset;
        while (nonSpaceIdx < end && s.charAt(nonSpaceIdx) == ' ') {
            nonSpaceIdx++;
        }

        int spaceCount = nonSpaceIdx - offset;
        if (spaceCount < prefixSpaces)
            return PARSE_FAILED;

        if (!s.startsWith(trimmedStartTxt, nonSpaceIdx))
            return PARSE_FAILED;

        return nonSpaceIdx + trimmedStartTxt.length();
    }

    @Override
    public boolean removeSpacesBefore() {
        return false;
    }

    @Override
    public LvLayoutTextNode clone() {
        try {
            return (LvLayoutTextNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return txt;
    }

    @Override
    public int search(String s, int offset, int end) {
        return s.indexOf(txt, offset);
    }
}
