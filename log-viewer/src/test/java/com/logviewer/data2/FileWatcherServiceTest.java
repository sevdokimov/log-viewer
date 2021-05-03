package com.logviewer.data2;

import com.google.common.collect.Sets;
import com.logviewer.utils.Destroyer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class FileWatcherServiceTest {

    private static final int DELAY = 100;

    @Test
    public void test() throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("log-test-");

        FileWatcherService ws = new FileWatcherService();

        try {
            Set<Path> changes = new HashSet<>();

            assert !isWatcherThreadStarted();

            Destroyer cancel = ws.watchDirectory(dir, changes::addAll);

            assert isWatcherThreadStarted();

            assert changes.isEmpty();

            Path newDir1 = dir.resolve("a.txt");
            Path newDir2 = dir.resolve("b.txt");
            Files.createFile(newDir1);
            Files.createFile(newDir2);

            Thread.sleep(DELAY);
            Assert.assertEquals(Sets.newHashSet(newDir1, newDir2), changes);
            changes.clear();

            Files.delete(newDir1);

            Thread.sleep(DELAY);
            Assert.assertEquals(Collections.singleton(newDir1), changes);
            changes.clear();

            cancel.close();

            Thread.sleep(DELAY);
            assert !isWatcherThreadStarted();

            Files.delete(newDir2);

            Thread.sleep(DELAY);
            assert changes.isEmpty();
        }
        finally {
            ws.destroy();
            FileSystemUtils.deleteRecursively(dir);
        }

    }

    @Test
    public void testDoubleRegistration() throws IOException, InterruptedException {
        FileWatcherService ws = new FileWatcherService();

        Path dir = Files.createTempDirectory("log-test-");

        try {
            List<Path> changes = new ArrayList<>();

            Consumer<List<Path>> watcher = changes::addAll;

            Destroyer destroyer1 = ws.watchDirectory(dir, watcher);
            Destroyer destroyer2 = ws.watchDirectory(dir, watcher); // register same listener second time

            Path file = dir.resolve("a.txt");
            Files.createFile(file);

            Thread.sleep(DELAY);
            Assert.assertEquals(Arrays.asList(file, file), changes);
            changes.clear();

            destroyer1.close();

            Files.delete(file);

            Thread.sleep(DELAY);
            Assert.assertEquals(Arrays.asList(file), changes);
            changes.clear();

            destroyer2.close();

            Thread.sleep(DELAY);
            assert !isWatcherThreadStarted();
            assert changes.isEmpty();
        }
        finally {
            FileSystemUtils.deleteRecursively(dir.toFile());
        }
    }

    public static boolean isWatcherThreadStarted() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.getName().equals(FileWatcherService.THREAD_NAME));
    }
}
