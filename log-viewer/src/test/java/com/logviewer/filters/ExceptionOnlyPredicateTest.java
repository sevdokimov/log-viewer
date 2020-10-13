package com.logviewer.filters;

import com.logviewer.data2.LvPredicateChecker;
import com.logviewer.data2.Record;
import org.junit.Test;
import org.mockito.Mockito;

public class ExceptionOnlyPredicateTest {

    @Test
    public void testMatching() {
        check("java.lang.NullPointerException\n" +
                "\tat com.xyz.Wombat(Wombat.java:57) ~[wombat-1.3.jar:1.3]\n" +
                "\tat  com.xyz.Wombat(Wombat.java:76) ~[wombat-1.3.jar:1.3]", true);
        check("java.lang.NullPointerException\n" +
                "\tat com.xyz.Wombat(Wombat.java:57) [wombat-1.3.jar:1.3]\n" +
                "\tat  com.xyz.Wombat(Wombat.java:76) [wombat-1.3.jar:1.3]", true);
        check("java.lang.NullPointerException\n" +
                "\tat com.xyz.Wombat(Wombat.java:57)\n" +
                "\tat  com.xyz.Wombat(Wombat.java:76)", true);

        check("java.lang.NullPointerException\n", false);
    }

    private static void check(String message, boolean expected) {
        ExceptionOnlyPredicate p = new ExceptionOnlyPredicate();

        Record record = new Record(message, System.currentTimeMillis(), 0, message.length(), false, new int[0]);

        assert p.test(record, Mockito.mock(LvPredicateChecker.class)) == expected;
    }
}