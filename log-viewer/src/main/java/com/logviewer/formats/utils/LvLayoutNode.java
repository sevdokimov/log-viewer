package com.logviewer.formats.utils;

public interface LvLayoutNode extends Cloneable {

    int SKIP_FIELD = Integer.MIN_VALUE;
    int PARSE_FAILED = Integer.MIN_VALUE + 1;

    /**
     *
     * @param s a string to parse.
     * @param offset begin of parsable text part
     * @param end end of parsable text part
     * @return the end of parsed field, or {@link #SKIP_FIELD} if the part is not present,
     *         or {@link #PARSE_FAILED} if the string is not a valid log line.
     */
    int parse(String s, int offset, int end);

    boolean removeSpacesBefore();

    LvLayoutNode clone();
}
