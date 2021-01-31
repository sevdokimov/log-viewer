package com.logviewer.logLibs.log4j;

import com.logviewer.utils.Triple;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copy pasted from `org.apache.logging.log4j.core.pattern.PatternParser`
 */
public class Log4jPatternParser {

    private static final char ESCAPE_CHAR = '%';

    private static final int DECIMAL = 10;

    /**
     * The states the parser can be in while parsing the pattern.
     */
    private enum ParserState {
        /**
         * Literal state.
         */
        LITERAL_STATE,

        /**
         * In converter name state.
         */
        CONVERTER_STATE,

        /**
         * Dot state.
         */
        DOT_STATE,

        /**
         * Min state.
         */
        MIN_STATE,

        /**
         * Max state.
         */
        MAX_STATE;
    }


    public static List<Triple<String, List<String>, FormattingInfo>> parse(@NonNull String pattern) {
        List<Triple<String, List<String>, FormattingInfo>> res = new ArrayList<>();

        final StringBuilder currentLiteral = new StringBuilder();

        final int patternLength = pattern.length();
        ParserState state = ParserState.LITERAL_STATE;
        char c;
        int i = 0;
        FormattingInfo formattingInfo = FormattingInfo.getDefault();

        while (i < patternLength) {
            c = pattern.charAt(i++);

            switch (state) {
                case LITERAL_STATE:

                    // In literal state, the last char is always a literal.
                    if (i == patternLength) {
                        currentLiteral.append(c);

                        continue;
                    }

                    if (c == ESCAPE_CHAR) {
                        // peek at the next char.
                        switch (pattern.charAt(i)) {
                            case ESCAPE_CHAR:
                                currentLiteral.append(c);
                                i++; // move pointer

                                break;

                            default:
                                if (currentLiteral.length() != 0) {
                                    res.add(stringLiteral(currentLiteral));
                                    currentLiteral.setLength(0);
                                }

                                currentLiteral.append(c); // append %
                                state = ParserState.CONVERTER_STATE;
                                formattingInfo = FormattingInfo.getDefault();
                        }
                    } else {
                        currentLiteral.append(c);
                    }

                    break;

                case CONVERTER_STATE:
                    currentLiteral.append(c);

                    switch (c) {
                        case '0':
                            // a '0' directly after the % sign indicates zero-padding
                            formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), true);
                            break;

                        case '-':
                            formattingInfo = new FormattingInfo(true, formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad());
                            break;

                        case '.':
                            state = ParserState.DOT_STATE;
                            break;

                        default:

                            if (c >= '0' && c <= '9') {
                                formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), c - '0',
                                        formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad());
                                state = ParserState.MIN_STATE;
                            } else {
                                i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, res);

                                // Next pattern is assumed to be a literal.
                                state = ParserState.LITERAL_STATE;
                                formattingInfo = FormattingInfo.getDefault();
                                currentLiteral.setLength(0);
                            }
                    } // switch

                    break;

                case MIN_STATE:
                    currentLiteral.append(c);

                    if (c >= '0' && c <= '9') {
                        // Multiply the existing value and add the value of the number just encountered.
                        formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength()
                                * DECIMAL + c - '0', formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad());
                    } else if (c == '.') {
                        state = ParserState.DOT_STATE;
                    } else {
                        i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, res);
                        state = ParserState.LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;

                case DOT_STATE:
                    currentLiteral.append(c);
                    switch (c) {
                        case '-':
                            formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(),false, formattingInfo.isZeroPad());
                            break;

                        default:

                            if (c >= '0' && c <= '9') {
                                formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                                        c - '0', formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad());
                                state = ParserState.MAX_STATE;
                            } else {
                                state = ParserState.LITERAL_STATE;
                            }
                    }

                    break;

                case MAX_STATE:
                    currentLiteral.append(c);

                    if (c >= '0' && c <= '9') {
                        // Multiply the existing value and add the value of the number just encountered.
                        formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                                formattingInfo.getMaxLength() * DECIMAL + c - '0', formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad());
                    } else {
                        i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, res);
                        state = ParserState.LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;
            } // switch
        }

        // while
        if (currentLiteral.length() != 0) {
            res.add(stringLiteral(currentLiteral));
        }

        return res;
    }

    private static int extractConverter(final char lastChar, final String pattern, final int start,
                                        final StringBuilder convBuf) {
        int i = start;
        convBuf.setLength(0);

        // When this method is called, lastChar points to the first character of the
        // conversion word. For example:
        // For "%hello" lastChar = 'h'
        // For "%-5hello" lastChar = 'h'
        // System.out.println("lastchar is "+lastChar);
        if (!Character.isUnicodeIdentifierStart(lastChar)) {
            return i;
        }

        convBuf.append(lastChar);

        while (i < pattern.length() && Character.isUnicodeIdentifierPart(pattern.charAt(i))) {
            convBuf.append(pattern.charAt(i));
            i++;
        }

        return i;
    }

    private static int extractOptions(final String pattern, final int start, final List<String> options) {
        int i = start;
        while (i < pattern.length() && pattern.charAt(i) == '{') {
            i++; // skip opening "{"
            final int begin = i; // position of first real char
            int depth = 1; // already inside one level
            while (depth > 0 && i < pattern.length()) {
                final char c = pattern.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    // TODO(?) maybe escaping of { and } with \ or %
                }
                i++;
            } // while

            if (depth > 0) { // option not closed, continue with pattern after closing bracket
                i = pattern.lastIndexOf('}');
                if (i == -1 || i < start) {
                    // if no closing bracket could be found or there is no closing bracket behind the starting
                    // character of our parsing process continue parsing after the first opening bracket
                    return begin;
                }
                return i + 1;
            }

            options.add(pattern.substring(begin, i - 1));
        } // while

        return i;
    }

    private static int finalizeConverter(final char c, final String pattern, final int start,
                                  final StringBuilder currentLiteral, final FormattingInfo formattingInfo,
                                  final List<Triple<String, List<String>, FormattingInfo>> res) {
        int i = start;
        final StringBuilder convBuf = new StringBuilder();
        i = extractConverter(c, pattern, i, convBuf);

        final String converterId = convBuf.toString();
        if (converterId.isEmpty()) {
            res.add(stringLiteral("%"));
            return i;
        }

        final List<String> options = new ArrayList<>();
        i = extractOptions(pattern, i, options);

        res.add(Triple.create(converterId, options, formattingInfo));

        currentLiteral.setLength(0);

        return i;
    }

    private static Triple<String, List<String>, FormattingInfo> stringLiteral(CharSequence str) {
        return Triple.create("", Collections.singletonList(str.toString()), FormattingInfo.getDefault());
    }

    public static class FormattingInfo {
        /**
         * Array of spaces.
         */
        private static final char[] SPACES = new char[] { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' };

        /**
         * Array of zeros.
         */
        private static final char[] ZEROS = new char[] { '0', '0', '0', '0', '0', '0', '0', '0' };

        /**
         * Default instance.
         */
        private static final FormattingInfo DEFAULT = new FormattingInfo(false, 0, Integer.MAX_VALUE, true);

        /**
         * Minimum length.
         */
        private final int minLength;

        /**
         * Maximum length.
         */
        private final int maxLength;

        /**
         * Alignment.
         */
        private final boolean leftAlign;

        /**
         * Left vs. right-hand side truncation.
         */
        private final boolean leftTruncate;

        /**
         * Use zero-padding instead whitespace padding
         */
        private final boolean zeroPad;

        /**
         * Creates new instance.
         *
         * @param leftAlign
         *            left align if true.
         * @param minLength
         *            minimum length.
         * @param maxLength
         *            maximum length.
         * @param leftTruncate
         *            truncates to the left if true
         */
        public FormattingInfo(final boolean leftAlign, final int minLength, final int maxLength, final boolean leftTruncate) {
            this(leftAlign, minLength, maxLength, leftTruncate, false);
        }

        /**
         * Creates new instance.
         *
         * @param leftAlign
         *            left align if true.
         * @param minLength
         *            minimum length.
         * @param maxLength
         *            maximum length.
         * @param leftTruncate
         *            truncates to the left if true
         * @param zeroPad
         *            use zero-padding instead of whitespace-padding
         */
        public FormattingInfo(final boolean leftAlign, final int minLength, final int maxLength, final boolean leftTruncate, final boolean zeroPad) {
            this.leftAlign = leftAlign;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.leftTruncate = leftTruncate;
            this.zeroPad = zeroPad;
        }

        /**
         * Gets default instance.
         *
         * @return default instance.
         */
        public static FormattingInfo getDefault() {
            return DEFAULT;
        }

        /**
         * Determine if left aligned.
         *
         * @return true if left aligned.
         */
        public boolean isLeftAligned() {
            return leftAlign;
        }

        /**
         * Determine if left truncated.
         *
         * @return true if left truncated.
         */
        public boolean isLeftTruncate() {
            return leftTruncate;
        }

        /**
         * Determine if zero-padded.
         *
         * @return true if zero-padded.
         */
        public boolean isZeroPad() {
            return zeroPad;
        }

        /**
         * Get minimum length.
         *
         * @return minimum length.
         */
        public int getMinLength() {
            return minLength;
        }

        /**
         * Get maximum length.
         *
         * @return maximum length.
         */
        public int getMaxLength() {
            return maxLength;
        }

        /**
         * Adjust the content of the buffer based on the specified lengths and alignment.
         *
         * @param fieldStart
         *            start of field in buffer.
         * @param buffer
         *            buffer to be modified.
         */
        public void format(final int fieldStart, final StringBuilder buffer) {
            final int rawLength = buffer.length() - fieldStart;

            if (rawLength > maxLength) {
                if (leftTruncate) {
                    buffer.delete(fieldStart, buffer.length() - maxLength);
                } else {
                    buffer.delete(fieldStart + maxLength, fieldStart + buffer.length());
                }
            } else if (rawLength < minLength) {
                if (leftAlign) {
                    final int fieldEnd = buffer.length();
                    buffer.setLength(fieldStart + minLength);

                    for (int i = fieldEnd; i < buffer.length(); i++) {
                        buffer.setCharAt(i, ' ');
                    }
                } else {
                    int padLength = minLength - rawLength;

                    final char[] paddingArray= zeroPad ? ZEROS : SPACES;

                    for (; padLength > paddingArray.length; padLength -= paddingArray.length) {
                        buffer.insert(fieldStart, paddingArray);
                    }

                    buffer.insert(fieldStart, paddingArray, 0, padLength);
                }
            }
        }

        /**
         * Returns a String suitable for debugging.
         *
         * @return a String suitable for debugging.
         */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("[leftAlign=");
            sb.append(leftAlign);
            sb.append(", maxLength=");
            sb.append(maxLength);
            sb.append(", minLength=");
            sb.append(minLength);
            sb.append(", leftTruncate=");
            sb.append(leftTruncate);
            sb.append(", zeroPad=");
            sb.append(zeroPad);
            sb.append(']');
            return sb.toString();
        }

    }
}
