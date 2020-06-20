package com.logviewer.web.session.tasks;

import com.logviewer.utils.Utils;

import java.io.Serializable;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SearchPattern implements Serializable {
    private String s;
    private boolean matchCase;
    private boolean regex;

    public SearchPattern(String s) {
        this(s, true, false);
    }

    public SearchPattern(String s, boolean matchCase, boolean regex) {
        this.s = s;
        this.matchCase = matchCase;
        this.regex = regex;
    }

    public Predicate<String> matcher() {
        if (regex) {
            Pattern pattern = Pattern.compile(s, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
            return str -> pattern.matcher(str).find();
        }

        if (matchCase)
            return str -> str.contains(s);

        return str -> Utils.containsIgnoreCase(str, s);
    }

    @Override
    public String toString() {
        if (!matchCase && !regex)
            return s;

        return "<" + (regex ? "R" : "") + (matchCase ? "I" : "") + "> " + s;
    }
}
