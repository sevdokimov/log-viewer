package com.logviewer;

import com.logviewer.data2.*;
import com.logviewer.data2.net.server.LogWatcherTask;
import com.logviewer.utils.Destroyer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.logviewer.TestUtils.MULTIFILE_LOG_FORMAT;
import static org.junit.Assert.*;

public class LogChangeListenerTest extends LogSessionTestBase {

    @Test
    public void testLocalFileLog() throws IOException, InterruptedException {
        Path tempFile = createTempFile();

        Log log = getLogService().openLog(tempFile.toString(), MULTIFILE_LOG_FORMAT);

        doTest(log, tempFile, Log.CHANGE_NOTIFICATION_TIMEOUT + 50);
    }

    @Test
    public void testRemoteLog() throws Exception {
        doRemoteTest((local, remote) -> {
            Path tempFile = createTempFile();

            LogView log = local.openRemoteLog(new LogPath(NODE, tempFile.toString())).get();

            doTest(log, tempFile, LogWatcherTask.DELAY);
        });
    }

    private void doTest(LogView log, Path file, long delay) throws IOException, InterruptedException {
        BlockingQueue<FileAttributes> queue = new LinkedBlockingQueue<>();

        Consumer<FileAttributes> listener = attr -> queue.add(attr == null ? new FileAttributes(0, 0) : attr);

        Destroyer destroyer1 = log.addChangeListener(listener);
        Destroyer destroyer2 = log.addChangeListener(listener);

        assertNull(queue.poll(delay, TimeUnit.MILLISECONDS)); // No event after initialization.

        Files.write(file, "150101 10:00:00 abc".getBytes());

        FileAttributes attr = queue.take();
        assertEquals(new FileAttributes(Files.readAttributes(file, BasicFileAttributes.class)), attr);
        assertNotNull(attr);
        assertEquals(attr, queue.take());

        destroyer2.close();
        Thread.sleep(delay); // removing of listener is not an instant operation.

        Files.write(file, "\n150101 10:00:01 abc".getBytes(), StandardOpenOption.APPEND);

        attr = queue.take();
        assertEquals(new FileAttributes(Files.readAttributes(file, BasicFileAttributes.class)), attr);

        assertNull(queue.poll(delay, TimeUnit.MILLISECONDS)); // Check that second listener was removed.

        Files.delete(file); // check file deletion
        attr = queue.take();
        assertEquals(0, attr.getModifiedTime());

        assertNull(queue.poll(delay, TimeUnit.MILLISECONDS)); // Check that second listener was removed.

        Files.write(file, "\n150101 10:00:01 abc".getBytes());
        attr = queue.take();
        assertEquals(new FileAttributes(Files.readAttributes(file, BasicFileAttributes.class)), attr);

        destroyer1.close();
        Thread.sleep(delay); // removing of listener is not an instant operation.

        Files.write(file, "\n150101 10:00:02 abc".getBytes(), StandardOpenOption.APPEND);

        assertNull(queue.poll(delay, TimeUnit.MILLISECONDS)); // Check that second listener was removed.

        assert !FileWatcherServiceTest.isWatcherThreadStarted();
    }
}
