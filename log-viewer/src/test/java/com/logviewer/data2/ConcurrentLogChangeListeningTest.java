package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConcurrentLogChangeListeningTest extends AbstractLogTest {

    @Test
    public void concurrentLogListening() throws IOException, InterruptedException {
        Path file = createTempFile();

        Log log1 = getLogService().openLog(file, LogService.DEFAULT_FORMAT);
        Log log2 = getLogService().openLog(file, new LogbackLogFormat("%d"));

        assert log1 != log2;

        BlockingQueue<FileAttributes> log1Changes = new LinkedBlockingQueue<>();
        log1.addChangeListener(log1Changes::add);

        BlockingQueue<FileAttributes> log2Changes = new LinkedBlockingQueue<>();
        log2.addChangeListener(log2Changes::add);

        assert log1Changes.isEmpty();
        assert log2Changes.isEmpty();

        Files.write(file, "22222\n".getBytes());

        assert log1Changes.poll(3, TimeUnit.SECONDS) != null;
        assert log2Changes.poll(3, TimeUnit.SECONDS) != null;
    }

}
