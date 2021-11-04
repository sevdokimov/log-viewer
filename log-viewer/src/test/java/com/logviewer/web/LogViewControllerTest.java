package com.logviewer.web;

import com.google.common.collect.Sets;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.LogView;
import com.logviewer.data2.net.Node;
import com.logviewer.data2.net.server.LogViewerBackdoorServer;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class LogViewControllerTest {

    @Test
    public void testSimpleCase() {
        check(Arrays.asList(new LogPath(null, "/opt/log-a.log"), new LogPath(null, "/opt/log-b.log")),
                "log-a.log", "log-b.log");
    }

    @Test
    public void testSameHost1() {
        check(Arrays.asList(new LogPath(new Node("aaa"), "/opt/log-a.log"), new LogPath(new Node("aaa"), "/opt/log-b.log")),
                "log-a.log", "log-b.log");
    }

    @Test
    public void testSameHost2() {
        check(Arrays.asList(new LogPath(new Node("aaa", 1313), "/opt/log-a.log"),
                        new LogPath(new Node("aaa", 1313), "/opt/log-b.log")),
                "log-a.log", "log-b.log");
    }

    @Test
    public void testSameName() {
        check(Arrays.asList(new LogPath(null, "/opt/aaa.log"), new LogPath(null, "/opt/xxx/aaa.log"),
                        new LogPath(null, "/opt/zzz/aaa.log")),
                
                "aaa.log", "zzz_aaa.log", "xxx_aaa.log");
    }

    @Test
    public void testDifferentPort() {
        check(Arrays.asList(new LogPath(new Node("zzz"), "/opt/log-a.log"), new LogPath(new Node("zzz", 1313), "/opt/log-b.log")),
                "zzz_log-a.log", "zzz-1313_log-b.log");
    }

    @Test
    public void testDefaultPort() {
        check(Arrays.asList(new LogPath(new Node("zzz"), "/opt/log-a.log"),
                        new LogPath(new Node("zzz", LogViewerBackdoorServer.DEFAULT_PORT), "/opt/log-b.log")),
                "zzz_log-a.log", "zzz_log-b.log");
    }

    @Test
    public void collision() {
        check(Arrays.asList(new LogPath(null, "/opt/zzz_log-a.log"),
                        new LogPath(new Node("zzz"), "/opt/log-a.log")),
                "zzz_log-a.log", "zzz_log-a.528125.log");
    }

    @Test
    public void collisionWrongExtension() {
        check(Arrays.asList(new LogPath(null, "/opt/zzz_log-a"),
                        new LogPath(new Node("zzz"), "/opt/log-a")),
                "zzz_log-a", "zzz_log-a-557351");
    }

    private static void check(List<LogPath> paths, String ... names) {
        List<LogView> logViews = paths.stream().map(p -> {
            LogView logView = Mockito.mock(LogView.class);
            Mockito.when(logView.getPath()).thenReturn(p);
            Mockito.when(logView.getId()).thenReturn(String.valueOf(p.hashCode() & 0x0FFFFF));

            return logView;
        }).collect(Collectors.toList());

        Map<String, LogView> map = LogViewController.assignUniqueNames(logViews);
        
        assertEquals(Sets.newHashSet(names), map.keySet());
    }

}