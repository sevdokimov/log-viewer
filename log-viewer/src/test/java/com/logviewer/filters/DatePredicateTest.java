package com.logviewer.filters;

import com.logviewer.data2.LvPredicateChecker;
import com.logviewer.data2.Record;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;

@SuppressWarnings("MagicConstant")
public class DatePredicateTest {

    @Test
    public void testEmpty() {
        check(new DatePredicate(0, true), new Date(111, 1, 1), true);
        check(new DatePredicate(0, false), new Date(111, 1, 1), true);
    }

    @Test
    public void testGreater() {
        check(new DatePredicate(new Date(111, 1, 4).getTime(), true), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 5).getTime(), true), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 6).getTime(), true), new Date(111, 1, 5), false);
    }

    @Test
    public void testLess() {
        check(new DatePredicate(new Date(111, 1, 4).getTime(), false), new Date(111, 1, 5), false);
        check(new DatePredicate(new Date(111, 1, 5).getTime(), false), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 6).getTime(), false), new Date(111, 1, 5), true);
    }

    private void check(DatePredicate predicate, Date recordDate, boolean expected) {
        String message = "aaa";
        Record record = new Record(message, recordDate.getTime(), 0, message.length(), false, new int[0]);

        assert predicate.test(record, Mockito.mock(LvPredicateChecker.class)) == expected;
    }
}