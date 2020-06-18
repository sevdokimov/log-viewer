package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;
import com.logviewer.web.session.tasks.SearchPattern;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class SubstringPredicate implements RecordPredicate {

    private final SearchPattern search;

    private transient Predicate<String> matcher;

    public SubstringPredicate(@Nonnull SearchPattern search) {
        this.search = search;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        Predicate<String> matcher = this.matcher;

        if (matcher == null) {
            matcher = search.matcher();
            this.matcher = matcher;
        }

        return matcher.test(record.getMessage());
    }
}
