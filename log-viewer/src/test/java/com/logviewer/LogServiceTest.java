package com.logviewer;

import com.google.common.collect.Iterables;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.LogView;
import com.logviewer.data2.net.Node;
import com.logviewer.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LogServiceTest extends LogSessionTestBase {

    @Test
    public void testDuplicateLog() throws Exception {
        doRemoteTest(MultifileConfiguration.class, (local, remote) -> {
            String file = getTestLog("Predicate.log");

            CompletableFuture<Map<String, LogView>> future = local.openLogs(Arrays.asList(
                    new LogPath(null, file),
                    new LogPath(null, file),
                    new LogPath(NODE, file),
                    new LogPath(NODE, file)
            ));

            Map<String, LogView> map = Utils.safeGet(future);
            assert map.size() == 1;
            assert Iterables.getOnlyElement(map.values()).getPath().getNode() == null;
        });
    }

    @Test
    public void testOpenLogInvalidHost() throws Exception {
        doRemoteTest(MultifileConfiguration.class, (local, remote) -> {
            String file = getTestLog("Predicate.log");

            CompletableFuture<Map<String, LogView>> future = local.openLogs(Arrays.asList(
                    new LogPath(null, file),
                    new LogPath(new Node("localhost", TEST_SERVER_PORT + 1), file)
            ));

            Map<String, LogView> map = future.get();
            Assert.assertEquals(2, map.size());
            Assert.assertEquals(1, map.values().stream().filter(it -> !it.isConnected()).count());
        });
    }
}
