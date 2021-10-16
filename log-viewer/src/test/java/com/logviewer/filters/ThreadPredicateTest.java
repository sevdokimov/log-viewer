package com.logviewer.filters;

import com.logviewer.data2.*;
import com.logviewer.utils.LvDateUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ThreadPredicateTest {

    @Test
    public void test() {
        check(new ThreadPredicate(), "zzz", true);
        check(new ThreadPredicate(new String[0], new String[0]), "xxx", true);
        check(new ThreadPredicate(new String[0], new String[]{"aaa"}), "xxx", true);
        check(new ThreadPredicate(new String[0], new String[]{"xxx"}), "xxx", false);
        check(new ThreadPredicate(null, new String[]{"xxx"}), "xxx", false);
        check(new ThreadPredicate(null, new String[]{"xxx-*"}), "xxx", true);
        check(new ThreadPredicate(null, new String[]{"xxx-*"}), "xxx-1", false);
        check(new ThreadPredicate(null, new String[]{"xxx-*"}), "xxx-", false);
        check(new ThreadPredicate(null, new String[]{"xxx-*"}), "xxx-100", false);
        check(new ThreadPredicate(null, new String[]{"*-fff"}), "xxx", true);
        check(new ThreadPredicate(null, new String[]{"*-fff"}), "fff", true);
        check(new ThreadPredicate(null, new String[]{"*-fff"}), "12-fff", false);

        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{"xxx-777"}), "fff", false);
        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{"xxx-777"}), "xxx", false);
        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{"xxx-777"}), "xxx-", true);
        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{"xxx-777"}), "xxx-100", true);
        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{"xxx-777"}), "xxx-777", false);
        check(new ThreadPredicate(new String[]{"xxx-*"}, new String[]{}), "xxx-777", true);
        check(new ThreadPredicate(new String[]{"xxx-*"}, null), "xxx-777", true);
        check(new ThreadPredicate(new String[]{"xxx-*", "yyy", "zzz"}, null), "xxx-777", true);
        check(new ThreadPredicate(new String[]{"xxx-*", "yyy", "zzz"}, null), "yyy", true);
        check(new ThreadPredicate(new String[]{"xxx-*", "yyy", "zzz"}, null), "zzz", true);
        check(new ThreadPredicate(new String[]{"xxx-*", "yyy", "zzz"}, null), "sfdfgdfgfdg", false);

        check(new ThreadPredicate(null, new String[]{"xxx-*", "yyy", "zzz"}), "sfdfgdfgfdg", true);
        check(new ThreadPredicate(null, new String[]{"xxx-*", "yyy", "zzz"}), "zzz", false);
        check(new ThreadPredicate(null, new String[]{"xxx-*", "yyy", "zzz"}), "yyy", false);
        check(new ThreadPredicate(null, new String[]{"xxx-*", "yyy", "zzz"}), "xxx-43", false);
    }

    private void check(ThreadPredicate p, String thread, boolean expected) {
        LogRecord record = new LogRecord(thread, LvDateUtils.toNanos(new Date()), 0, thread.length(), false,
                new int[]{0, thread.length()}, Collections.singletonMap("t", 0));

        LogFormat.FieldDescriptor[] fields = new LogFormat.FieldDescriptor[]{new DefaultFieldDesciptor("t", FieldTypes.THREAD)};
        LogFilterContext context = Mockito.mock(LogFilterContext.class);
        Mockito.when(context.getFields()).thenReturn(fields);

        assertEquals(expected, p.test(record, context));
    }
}