package com.logviewer.filters;

import com.logviewer.data2.LogRecord;
import com.logviewer.data2.LvPredicateChecker;
import com.logviewer.utils.LvDateUtils;
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
        check(new DatePredicate(new Date(111, 1, 4), true), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 5), true), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 6), true), new Date(111, 1, 5), false);
    }

    @Test
    public void testLess() {
        check(new DatePredicate(new Date(111, 1, 4), false), new Date(111, 1, 5), false);
        check(new DatePredicate(new Date(111, 1, 5), false), new Date(111, 1, 5), true);
        check(new DatePredicate(new Date(111, 1, 6), false), new Date(111, 1, 5), true);
    }

    private void check(DatePredicate predicate, Date recordDate, boolean expected) {
        String message = "aaa";
        LogRecord record = new LogRecord(message, LvDateUtils.toNanos(recordDate), 0, message.length(), message.length());

        assert predicate.test(record, Mockito.mock(LvPredicateChecker.class)) == expected;
    }
}