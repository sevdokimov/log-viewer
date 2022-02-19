package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;

public class LvLayoutTextNode implements LvLayoutNode, LvLayoutNodeSearchable, Cloneable {

    protected final String txt;

    private final String trimmedStartTxt;

    protected final int prefixSpaces;

    private LvLayoutTextNode(String txt, String trimmedStartTxt, int prefixSpaces) {
        this.txt = txt;
        this.trimmedStartTxt = trimmedStartTxt;
        this.prefixSpaces = prefixSpaces;
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

        if (end - nonSpaceIdx < trimmedStartTxt.length() || !s.startsWith(trimmedStartTxt, nonSpaceIdx))
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

    public static LvLayoutTextNode of(@NonNull String txt) {
        assert txt.length() > 0;

        int prefixSpaces = 0;
        while (true) {
            if (prefixSpaces == txt.length()) {
                return new LvLayoutTextNode(txt, "", prefixSpaces) {
                    @Override
                    public int parse(String s, int offset, int end) {
                        int next = offset + this.prefixSpaces;

                        if (next <= end && s.startsWith(this.txt, offset))
                            return next;

                        return PARSE_FAILED;
                    }
                };
            }

            if (txt.charAt(prefixSpaces) != ' ')
                break;

            prefixSpaces++;
        }

        if (prefixSpaces == 0 && txt.length() == 1) {
            return new LvLayoutTextNode(txt, txt, 0) {

                private final char a = this.txt.charAt(0);

                @Override
                public int parse(String s, int offset, int end) {
                    int next = offset + 1;
                    if (next <= end && s.charAt(offset) == a)
                        return next;
                    
                    return PARSE_FAILED;
                }

                @Override
                public boolean removeSpacesBefore() {
                    return true;
                }
            };
        }

        return new LvLayoutTextNode(txt, txt.substring(prefixSpaces), prefixSpaces);
    }


}
