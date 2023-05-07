package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.formats.SimpleLogFormat;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.utils.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class LogTest extends AbstractLogTest {

    @Test
    public void loadContentSecurity() {
        LvFileAccessManagerImpl accessManager = getCommonContext().getBean(LvFileAccessManagerImpl.class);
        accessManager.setVisibleFiles(Collections.singletonList(Paths.get("/tmp/unexisting.log")));

        Path file = Paths.get(getTestLog("utf8.log"));

        Log log = getLogService().openLog(file, new SimpleLogFormat(StandardCharsets.UTF_8));

        CompletableFuture<Pair<String, Integer>> future = log.loadContent(0, 10);

        ExecutionException exception = TestUtils.assertError(ExecutionException.class, future::get);

        Assertions.assertThat(exception.getCause()).isInstanceOf(SecurityException.class);
    }

    @Test
    public void loadContentUtf8() throws IOException, ExecutionException, InterruptedException {
        Path file = Paths.get(getTestLog("utf8.log"));

        byte[] data = Files.readAllBytes(file);

        int nonLatinCharOffset = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0) {
                assert data[i + 1] < 0;
                nonLatinCharOffset = i;
                break;
            }
        }

        assert nonLatinCharOffset >= 0;

        Log log = getLogService().openLog(file, new SimpleLogFormat(StandardCharsets.UTF_8));

        Pair<String, Integer> pair = log.loadContent(0, nonLatinCharOffset + 1).get();

        int dataLen = pair.getSecond();

        assertEquals(nonLatinCharOffset, dataLen);
        assertEquals(new String(Arrays.copyOf(data, dataLen), StandardCharsets.UTF_8), pair.getFirst());
    }
}