package com.logviewer.utils;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;
import com.logviewer.filters.RecordPredicate;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TestPredicate implements RecordPredicate {

    public static final long WAIT_TIMEOUT = Integer.getInteger("test-wait-timeout", 5) * 1000;

    private static final Map<Object, Predicate<LogRecord>> lockMap = new LinkedHashMap<>();

    private static final List<LogRecord> passed = new ArrayList<>();

    private static final Set<LogRecord> waited = new HashSet<>();

    public static void handle(LogRecord record) {
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
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        handle(record);
        return true;
    }

    public static boolean wasProcessed(String record) {
        return wasProcessed(r -> r.getMessage().equals(record));
    }

    public static boolean wasProcessed(Predicate<LogRecord> p) {
        synchronized (passed) {
            return passed.stream().anyMatch(p);
        }
    }

    public static List<LogRecord> getPassed() {
        synchronized (passed) {
            return new ArrayList<>(passed);
        }
    }

    public static void waitForRecord(@NonNull String record) throws InterruptedException {
        waitForRecord(r -> r.getMessage().equals(record));
    }

    public static void waitForRecord(Predicate<LogRecord> p) throws InterruptedException {
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

    public static synchronized Object lock(Predicate<LogRecord> p) {
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

    public static synchronized void waitForLocked(Predicate<LogRecord> predicate) throws InterruptedException {
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
