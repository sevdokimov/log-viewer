package com.logviewer.filters;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import com.logviewer.utils.RegexUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ThreadPredicate implements RecordPredicate {

    private String[] includes;
    private String[] excludes;

    private transient volatile Predicate<String>[] includePredicate;
    private transient volatile Predicate<String>[] excludePredicate;

    public ThreadPredicate() {

    }

    public ThreadPredicate(String[] includes, String[] excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    private Predicate<String> toPredicate(@NonNull String thread) {
        int idx = thread.indexOf('*');
        if (idx < 0)
            return t -> t.equals(thread);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < thread.length(); i++) {
            char a = thread.charAt(i);
            if (a == '*') {
                sb.append(".*");
            } else {
                RegexUtils.escapePattern(sb, a);
            }
        }

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE).asPredicate();
    }

    @Nullable
    private Predicate<String>[] getIncludePredicate() {
        if (includes == null)
            return null;

        Predicate<String>[] res = this.includePredicate;
        if (res == null) {
            res = new Predicate[includes.length];
            for (int i = 0; i < includes.length; i++) {
                res[i] = toPredicate(includes[i]);
            }

            this.includePredicate = res;
        }

        return res;
    }

    @Nullable
    private Predicate<String>[] getExcludePredicate() {
        if (excludes == null)
            return null;

        Predicate<String>[] res = this.excludePredicate;
        if (res == null) {
            res = new Predicate[excludes.length];
            for (int i = 0; i < excludes.length; i++) {
                res[i] = toPredicate(excludes[i]);
            }

            this.excludePredicate = res;
        }

        return res;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        LogFormat.FieldDescriptor[] fields = ctx.getFields();
        for (int i = 0; i < fields.length; i++) {
            LogFormat.FieldDescriptor field = fields[i];
            if (FieldTypes.is(field.type(), FieldTypes.THREAD)) {
                String fieldValue = record.getFieldText(i);
                if (fieldValue == null || fieldValue.isEmpty())
                    continue;

                Predicate<String>[] includePredicate = getIncludePredicate();
                if (includePredicate != null && includePredicate.length > 0) {
                    if (!anyMatches(includePredicate, fieldValue))
                        return false;
                }

                Predicate<String>[] excludePredicate = getExcludePredicate();
                if (excludePredicate != null) {
                    if (anyMatches(excludePredicate, fieldValue))
                        return false;
                }
            }
        }

        return true;
    }

    private boolean anyMatches(@NonNull Predicate<String>[] predicates, @NonNull String thread) {
        for (Predicate<String> predicate : predicates) {
            if (predicate.test(thread))
                return true;
        }

        return false;
    }

    public String[] getIncludes() {
        return includes;
    }

    public ThreadPredicate setIncludes(String[] includes) {
        this.includes = includes;
        return this;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public ThreadPredicate setExcludes(String[] excludes) {
        this.excludes = excludes;
        return this;
    }
}
