package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    public List<RecordPredicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
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
}
