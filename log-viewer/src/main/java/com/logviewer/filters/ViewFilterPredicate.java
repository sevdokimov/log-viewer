package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class ViewFilterPredicate implements RecordPredicate {

    private final RecordPredicate[] filters;

    public ViewFilterPredicate(@NonNull RecordPredicate[] filters) {
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


        RecordPredicate[] recordPredicates = new RecordPredicate[viewFilterPredicate.getFilters().length + predicates.length];
        System.arraycopy(viewFilterPredicate.getFilters(), 0, recordPredicates, 0, viewFilterPredicate.getFilters().length);
        System.arraycopy(predicates, 0, recordPredicates, viewFilterPredicate.getFilters().length, predicates.length);
        
        return new ViewFilterPredicate(recordPredicates);
    }
}
