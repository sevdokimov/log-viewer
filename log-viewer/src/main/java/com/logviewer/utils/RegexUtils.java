package com.logviewer.utils;

import org.springframework.lang.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    private static final Pattern SIMPLE_DATE_FORMAT_REGEX = Pattern.compile("'([^']*)'|(([a-zA-Z])\\3*)");

    private static final String ESCAPED_CHARACTERS = "\\[(){}.?*+^$#|";

    private RegexUtils() {
    }

    public static void escapePattern(StringBuilder sb, String text) {
        for (int i = 0, len = text.length(); i < len; i++) {
            char a = text.charAt(i);
            if (ESCAPED_CHARACTERS.indexOf(a) > 0) {
                sb.append('\\');
            }

            sb.append(a);
        }
    }

    public static Pattern dateFormatToRegex(String pattern) {
        StringBuilder res = new StringBuilder();
        dateFormatToRegex(res, pattern);
        return Pattern.compile(res.toString());
    }

    public static void dateFormatToRegex(StringBuilder res, String pattern) {
        Matcher matcher = SIMPLE_DATE_FORMAT_REGEX.matcher(pattern);

        int idx = 0;

        while (matcher.find()) {
            escapePattern(res, pattern.substring(idx, matcher.start()));

            String escapedText = matcher.group(1);
            if (escapedText != null) {
                if (escapedText.length() == 0)
                    res.append('\'');
                else
                    escapePattern(res, escapedText);
            }
            else {
                String p = matcher.group(2);

                switch (p.charAt(0)) {
                    case 'G':
                        res.append("AD");
                        break;

                    case 'y':
                    case 'Y':
                        if (p.length() == 2)
                            res.append("[0912]\\d");
                        else {
                            for (int i = 4; i < p.length(); i++)
                                res.append('0');

                            res.append("(?:19|20)\\d\\d");
                        }
                        break;

                    case 'M':
                        if (p.length() == 1) {
                            res.append("(?:[1-9]|1[012])");
                        }
                        else if (p.length() == 2) {
                            res.append("(?:0[1-9]|1[012])");
                        }
                        else if (p.length() == 3) {
                            res.append("\\p{Lu}\\p{IsAlphabetic}\\p{IsAlphabetic}");
                        }
                        else {
                            res.append("\\p{Lu}\\p{IsAlphabetic}+");
                        }

                        break;

                    case 'w':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("[0-5]\\d");
                        }
                        else {
                            res.append("(?:[1-9]|[1-5]\\d)");
                        }
                        break;

                    case 'W':
                        addZeros(res, p.length() - 1);
                        res.append("[1-5]");
                        break;

                    case 'd':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("(?:0[1-9]|[12]\\d|3[01])");
                        }
                        else {
                            res.append("(?:[1-9]|[12]\\d|3[01])");
                        }
                        break;

                    case 'D':
                        addZeros(res, p.length() - 3);
                        if (p.length() >= 3) {
                            res.append("[0-3]\\d\\d");
                        }
                        else {
                            res.append("\\d{").append(p.length()).append(",3}");
                        }
                        break;

                    case 'F':
                        res.append("\\d\\d?");
                        break;

                    case 'E':
                        if (p.length() >= 4) {
                            res.append("\\p{Lu}\\p{IsAlphabetic}+");
                        }
                        else {
                            res.append("\\p{Lu}\\p{IsAlphabetic}\\p{IsAlphabetic}");
                        }
                        break;

                    case 'u':
                        res.append("[1-7]");
                        break;

                    case 'a':
                        res.append("(?:AM|PM)");
                        break;

                    case 'H':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("(?:[01]\\d|2[0-3])");
                        }
                        else {
                            res.append("(?:1?\\d|2[0-3])");
                        }
                        break;

                    case 'k':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("(?:0[1-9]|1\\d|2[0-4])");

                        }
                        else {
                            res.append("(?:[1-9]|1\\d|2[0-4])");
                        }
                        break;

                    case 'K':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("(?:0\\d|1[01])");

                        }
                        else {
                            res.append("(?:\\d|1[01])");
                        }
                        break;

                    case 'h':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("(?:0[1-9]|1[0-2])");

                        }
                        else {
                            res.append("(?:[1-9]|1[0-2])");
                        }
                        break;

                    case 's':
                    case 'm':
                        addZeros(res, p.length() - 2);
                        if (p.length() >= 2) {
                            res.append("[0-5]\\d");
                        }
                        else {
                            res.append("[1-5]?\\d");
                        }
                        break;

                    case 'S':
                        if (p.length() >= 3) {
                            addZeros(res, p.length() - 3);
                            res.append("\\d\\d\\d");
                        }
                        else if (p.length() == 2) {
                            res.append("(?:0\\d|[1-9]\\d{1,2})");
                        }
                        else {
                            res.append("(?:0|[1-9]\\d{1,2})");
                        }
                        break;

                    case 'z':
                        if (p.length() > 3) {
                            res.append("\\p{Lu}\\p{IsAlphabetic}+ \\p{Lu}\\p{IsAlphabetic}+ \\p{Lu}\\p{IsAlphabetic}+");
                        }
                        else {
                            res.append("[A-Z]{3}");
                        }
                        break;

                    case 'Z':
                        res.append("[+-]\\d\\d\\d\\d");
                        break;

                    case 'X':
                        if (p.length() == 1) {
                            res.append("(?:Z|[+-]\\d\\d)");
                        }
                        else if (p.length() == 2) {
                            res.append("(?:Z|[+-]\\d\\d\\d\\d)");
                        }
                        else if (p.length() == 3) {
                            res.append("(?:Z|[+-]\\d\\d:\\d\\d)");
                        }

                        break;


                    default:
                        throw new IllegalArgumentException("Data pattern is invalid: " + pattern);
                }
            }

            idx = matcher.end();
        }

        escapePattern(res, pattern.substring(idx));
    }

    private static void addZeros(StringBuilder res, int n) {
        for (int i = 0; i < n; i++) {
            res.append('0');
        }
    }

    public static Pattern filePattern(@NonNull String filePattern) {
        StringBuilder sb = new StringBuilder();

        filePattern = Utils.normalizePath(filePattern).replaceAll("\\*{3,}", "**");

        int i = 0;

        if (filePattern.startsWith("**/")) {
            sb.append("(?:.*[/\\\\]+)?");
            i = 3;
        }

        for (int len = filePattern.length(); i < len; i++) {
            char a = filePattern.charAt(i);

            if (a == '*') {
                if (filePattern.startsWith("**", i)) {
                    sb.append(".*");
                    i++;
                } else {
                    sb.append("[^/\\\\]*");
                }
            } else if (a == '/') {
                sb.append("[/\\\\]+");

                if (filePattern.startsWith("/**/", i)) {
                    sb.append("(?:.+[/\\\\]+)*");
                    i += 3;
                }
            } else if (ESCAPED_CHARACTERS.indexOf(a) > 0) {
                sb.append('\\').append(a);
            } else {
                sb.append(a);
            }
        }

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
