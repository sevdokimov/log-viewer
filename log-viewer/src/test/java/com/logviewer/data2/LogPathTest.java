package com.logviewer.data2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class LogPathTest {

    @Test
    public void parseNoHost() {
        List<LogPath> logPaths = LogPath.parsePathFromHttpParameter("/aaa/b.log");
        LogPath element = Iterables.getOnlyElement(logPaths);
        assertThat(element.getNode(), nullValue());
        assertThat(element.getFile(), is("/aaa/b.log"));
    }

    @Test
    public void parseOneHost() {
        List<LogPath> logPaths = LogPath.parsePathFromHttpParameter("/aaa/b.log@my-host");
        LogPath element = Iterables.getOnlyElement(logPaths);
        assertThat(element.getNode().getHost(), is("my-host"));
        assertThat(element.getFile(), is("/aaa/b.log"));
    }

    @Test
    public void parseThreeHosts() {
        List<LogPath> logPaths = LogPath.parsePathFromHttpParameter("/aaa/b.log@my-host-1,my-host-2,my-host-3");
        assertThat(logPaths.size(), is(3));

        assertThat(logPaths.stream().map(p -> p.getNode().getHost()).collect(Collectors.toList()),
                is(Arrays.asList("my-host-1", "my-host-2", "my-host-3")));

        assertThat(logPaths.stream().map(p -> p.getFile()).collect(Collectors.toSet()),
                is(Sets.newHashSet("/aaa/b.log")));
    }
}