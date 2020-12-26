package com.logviewer.utils;

import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.DatePredicate;
import com.logviewer.filters.FieldValueSetPredicate;
import com.logviewer.filters.RecordPredicate;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@SuppressWarnings("ConstantConditions")
public class PredicateUtilsTest {

    private static final long A = new Date(111, Calendar.JANUARY, 1).getTime();
    private static final long B = new Date(111, Calendar.JANUARY, 2).getTime();
    private static final long C = new Date(111, Calendar.JANUARY, 3).getTime();

    @Test
    public void testSimple1() {
        RecordPredicate pr = new DatePredicate(B, true);

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == B;
    }

    @Test
    public void testNullPredicate() {
        RecordPredicate pr = new DatePredicate(0, true);

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;

        pr = new DatePredicate(0, false);

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

    @Test
    public void testSimple2() {
        RecordPredicate pr = new DatePredicate(B, false);

        assert PredicateUtils.extractTimeLimit(pr, true) == B;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

    @Test
    public void testRangeAnd() {
        RecordPredicate pr = new CompositeRecordPredicate(true, new DatePredicate(A, true), new DatePredicate(C, false),
                new FieldValueSetPredicate("aaa", Collections.emptyList()));

        assert PredicateUtils.extractTimeLimit(pr, true) == C;
        assert PredicateUtils.extractTimeLimit(pr, false) == A;
    }

    @Test
    public void testRangeAnd2() {
        RecordPredicate pr = new CompositeRecordPredicate(true, new DatePredicate(A, true), new DatePredicate(B, true));

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == B;
    }

    @Test
    public void testRangeOr() {
        RecordPredicate pr = new CompositeRecordPredicate(false, new DatePredicate(A, true), new DatePredicate(C, false));

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

    @Test
    public void testRangeOr2() {
        RecordPredicate pr = new CompositeRecordPredicate(false, new DatePredicate(A, true), new FieldValueSetPredicate("aaa", Collections.emptyList()));

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

    @Test
    public void testRangeOr3() {
        RecordPredicate pr = new CompositeRecordPredicate(false, new DatePredicate(A, true), new DatePredicate(B, true));

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

    @Test
    public void testRangeOrNot() {
        RecordPredicate pr = new CompositeRecordPredicate(false, new DatePredicate(A, false), new DatePredicate(C, true)).not();

        assert PredicateUtils.extractTimeLimit(pr, true) == null;
        assert PredicateUtils.extractTimeLimit(pr, false) == null;
    }

}