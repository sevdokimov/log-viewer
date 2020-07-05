package com.logviewer.tests.web;

import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.FieldArgPredicate;
import com.logviewer.filters.RecordPredicate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestPredicates {

    public static final RecordPredicate ODD = new CompositeRecordPredicate(false,
            new FieldArgPredicate("_", "1", FieldArgPredicate.Operator.END_WITH),
            new FieldArgPredicate("_", "3", FieldArgPredicate.Operator.END_WITH),
            new FieldArgPredicate("_", "5", FieldArgPredicate.Operator.END_WITH),
            new FieldArgPredicate("_", "7", FieldArgPredicate.Operator.END_WITH),
            new FieldArgPredicate("_", "9", FieldArgPredicate.Operator.END_WITH));

    public static RecordPredicate recordValues(String ... values) {
        List<RecordPredicate> list = Stream.of(values).map(v -> new FieldArgPredicate("_", v)).collect(Collectors.toList());
        return new CompositeRecordPredicate(false, list);
    }

}
