package com.logviewer.utils;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;
import com.logviewer.filters.RecordPredicate;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TestPredicate implements RecordPredicate {

    public static final long WAIT_TIMEOUT = Integer.getInteger("test-wait-timeout", 5) * 1000;

    private static final Map<Object, Predicate<Record>> lockMap = new LinkedHashMap<>();

    private static final List<Record> passed = new ArrayList<>();

    private static final Set<Record> waited = new HashSet<>();

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        try {
            synchronized (TestPredicate.class) {
                boolean added = false;

                try {
                    while (lockMap.values().stream().anyMatch(p -> p.test(record))) {
                        if (!added) {
                            waited.add(record);
                            added = true;
                        }

                        TestPredicate.class.notifyAll();
                        TestPredicate.class.wait();
                    }

                    synchronized (passed) {
                        passed.add(record);
                        passed.notifyAll();
                    }
                } finally {
                    if (added)
                        waited.remove(record);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }

        return false;
    }

    public static boolean wasProcessed(String record) {
        return wasProcessed(r -> r.getMessage().equals(record));
    }

    public static boolean wasProcessed(Predicate<Record> p) {
        synchronized (passed) {
            return passed.stream().anyMatch(p);
        }
    }

    public static void waitForRecord(@Nonnull String record) throws InterruptedException {
        waitForRecord(r -> r.getMessage().equals(record));
    }

    public static void waitForRecord(Predicate<Record> p) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        synchronized (passed) {
            while (true) {
                if (passed.stream().anyMatch(p))
                    return;

                passed.wait(500);

                if ((System.currentTimeMillis() - startTime) > WAIT_TIMEOUT)
                    throw new RuntimeException();
            }
        }
    }

    public static synchronized Object lock(Pattern pattern) {
        return lock(record -> pattern.matcher(record.getMessage()).matches());
    }

    public static synchronized Object lock(String line) {
        return lock(record -> record.getMessage().equals(line));
    }

    public static synchronized Object lock(Predicate<Record> p) {
        Object key = new Object();
        lockMap.put(key, p);
        return key;
    }

    public static synchronized boolean unlock(Object key) {
        if (lockMap.remove(key) != null) {
            TestPredicate.class.notifyAll();
            return true;
        }

        return false;
    }

    public static void waitForLocked(String line) throws InterruptedException {
        waitForLocked(record -> record.getMessage().equals(line));
    }

    public static synchronized void waitForLocked(Predicate<Record> predicate) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (waited.stream().anyMatch(predicate))
                return;

            TestPredicate.class.wait(500);

            if ((System.currentTimeMillis() - startTime) > WAIT_TIMEOUT)
                throw new RuntimeException();
        }
    }

    public static synchronized void clear() {
        lockMap.clear();
        TestPredicate.class.notifyAll();

        synchronized (passed) {
            passed.clear();
            passed.notifyAll();
        }
    }
}
