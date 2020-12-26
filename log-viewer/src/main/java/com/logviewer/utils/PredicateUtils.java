package com.logviewer.utils;

import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.DatePredicate;
import com.logviewer.filters.RecordPredicate;
import org.springframework.lang.Nullable;

import java.util.List;

public class PredicateUtils {

    public static Long extractTimeLimit(@Nullable RecordPredicate filter, boolean isUpperLimit) {
        if (filter == null)
            return null;

        if (filter instanceof DatePredicate) {
            DatePredicate datePredicate = (DatePredicate) filter;

            if (datePredicate.getDate() == 0)
                return null;

            if (datePredicate.isGreater() != isUpperLimit) {
                return datePredicate.getDate();
            }
        } else if (filter instanceof CompositeRecordPredicate) {
            CompositeRecordPredicate comp = (CompositeRecordPredicate) filter;

            List<RecordPredicate> predicates = comp.getPredicates();
            if (predicates.isEmpty() || !comp.isAnd())
                return null;

            Long res = null;

            for (RecordPredicate predicate : comp.getPredicates()) {
                Long limit = extractTimeLimit(predicate, isUpperLimit);

                if (limit != null) {
                    if (res == null) {
                        res = limit;
                    } else {
                        res = isUpperLimit ? Math.min(res, limit) : Math.max(res, limit);
                    }
                }
            }

            return res;
        }
        
        return null;
    }

}
