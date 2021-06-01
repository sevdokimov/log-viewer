package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 *
 */
public class CompositeRecordPredicate implements RecordPredicate {

    private final List<RecordPredicate> predicates = new ArrayList<>();

    private boolean isAnd;

    public CompositeRecordPredicate() {

    }

    public CompositeRecordPredicate(boolean isAnd, Collection<RecordPredicate> predicates) {
        this.isAnd = isAnd;
        this.predicates.addAll(predicates);
    }

    public CompositeRecordPredicate(boolean isAnd, RecordPredicate ... predicates) {
        this.isAnd = isAnd;
        Collections.addAll(this.predicates, predicates);
    }

    public void addPredicate(RecordPredicate recordPredicate) {
        predicates.add(recordPredicate);
    }

    public boolean isAnd() {
        return isAnd;
    }

    public List<RecordPredicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        if (isAnd) {
            for (RecordPredicate predicate : predicates) {
                if (!predicate.test(record, ctx))
                    return false;
            }

            return true;
        }
        else {
            for (RecordPredicate predicate : predicates) {
                if (predicate.test(record, ctx))
                    return true;
            }

            return false;
        }
    }

    @Nullable
    public static RecordPredicate and(@Nullable RecordPredicate ... filters) {
        if (filters == null || filters.length == 0)
            return null;

        return and(Arrays.asList(filters));
    }

    public static RecordPredicate and(@Nullable Collection<RecordPredicate> filters) {
        if (filters == null || filters.isEmpty())
            return null;

        if (filters.size() == 1)
            return filters.iterator().next();
        
        return new CompositeRecordPredicate(true, filters);
    }
}
