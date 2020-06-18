package com.logviewer.filters;

import com.google.common.collect.ObjectArrays;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ViewFilterPredicate implements RecordPredicate {

    private final RecordPredicate[] filters;

    public ViewFilterPredicate(@Nonnull RecordPredicate[] filters) {
        this.filters = filters;
    }

    public RecordPredicate[] getFilters() {
        return filters;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        for (RecordPredicate filter : filters) {
            if (filter.test(record, ctx))
                return false;
        }

        return true;
    }

    public static ViewFilterPredicate addPredicate(@Nullable ViewFilterPredicate viewFilterPredicate, @Nullable RecordPredicate ... predicates) {
        if (predicates == null || predicates.length == 0)
            return viewFilterPredicate;

        if (viewFilterPredicate == null)
            return new ViewFilterPredicate(predicates);

        RecordPredicate[] recordPredicates = ObjectArrays.concat(viewFilterPredicate.getFilters(), predicates, RecordPredicate.class);
        return new ViewFilterPredicate(recordPredicates);
    }
}
