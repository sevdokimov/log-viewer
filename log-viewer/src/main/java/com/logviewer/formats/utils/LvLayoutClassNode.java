package com.logviewer.formats.utils;

public class LvLayoutClassNode implements LvLayoutNode {

    private static final int STATE_INIT = 0;
    private static final int STATE_AFTER_DOT = 1;
    private static final int STATE_LITERAL_MIDDLE = 2;
    private static final int STATE_FINISH = 3;

    @Override
    public int parse(String s, int offset, int end) {
        int state = STATE_INIT;

        int i;

        for (i = offset; i < end; i++) {
            char a = s.charAt(i);

            switch (state) {
                case STATE_INIT:
                    if (a == '.') {
                        state = STATE_AFTER_DOT; // may start with '.' if the field has length limit
                    } else if (Character.isJavaIdentifierStart(a)) {
                        state = STATE_LITERAL_MIDDLE;
                    } else {
                        return PARSE_FAILED;
                    }

                    break;

                case STATE_AFTER_DOT:
                    if (Character.isJavaIdentifierStart(a)) {
                        state = STATE_LITERAL_MIDDLE;
                    } else {
                        if (i - offset <= 3)
                            return PARSE_FAILED;

                        return i;
                    }

                    break;

                case STATE_LITERAL_MIDDLE:
                    if (a == '.') {
                        state = STATE_AFTER_DOT;
                    } else if (!Character.isJavaIdentifierPart(a)) {
                        state = STATE_FINISH;
                    }

                    break;

                default:
                    throw new IllegalStateException();
            }

            if (state == STATE_FINISH)
                break;
        }

        if (end == offset)
            return PARSE_FAILED;

        if (i - offset <= 3 && s.charAt(offset) == '.') {
            return PARSE_FAILED;
        }

        return i;
    }

    @Override
    public LvLayoutClassNode clone() {
        return new LvLayoutClassNode();
    }
}
