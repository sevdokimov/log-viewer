package com.logviewer.tests;

import com.logviewer.HoconPathResolver;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.net.Node;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.lang.NonNull;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;

public class HoconPathResolverTest {

    @Test
    public void test() {
        check("log-paths: {path = { " +
                "file: \"/logs/xxx\"" +
                "}\n}",
                new LogPath(null, "/logs/xxx"));

        check("log-paths: {path = { " +
                "file: [\"/logs/xxx\", \"/logs/yyy\", \"/logs/zzz\" ]" +
                "}\n}",
                new LogPath(null, "/logs/xxx"), new LogPath(null, "/logs/yyy"), new LogPath(null, "/logs/zzz"));

        check("log-paths: {path = { " +
                        "file: \"/logs/xxx\"\n" +
                        "host: host-01" +
                        "}\n}",
                new LogPath(new Node("host-01"), "/logs/xxx"));

        check("log-paths: {path = { " +
                        "file: \"/logs/xxx\"\n" +
                        "host: [host-01, host-02, host-03]" +
                        "}\n}",
                new LogPath(new Node("host-01"), "/logs/xxx"), new LogPath(new Node("host-02"), "/logs/xxx"),
                new LogPath(new Node("host-03"), "/logs/xxx"));

        check("log-paths: {path = { " +
                        "file: [\"/logs/xxx\", \"/logs/yyy\" ]\n" +
                        "host: [host-01, host-02]" +
                        "}\n}",
                new LogPath(new Node("host-01"), "/logs/xxx"),
                new LogPath(new Node("host-02"), "/logs/xxx"),
                new LogPath(new Node("host-01"), "/logs/yyy"),
                new LogPath(new Node("host-02"), "/logs/yyy"));

        check("log-paths: {path = {" +
                        "port=9090\n" +
                        "file: [\"/logs/xxx\", \"/logs/yyy\" ]\n" +
                        "host: [host-01, host-02]" +
                        "}\n}",
                new LogPath(new Node("host-01", 9090), "/logs/xxx"),
                new LogPath(new Node("host-02", 9090), "/logs/xxx"),
                new LogPath(new Node("host-01", 9090), "/logs/yyy"),
                new LogPath(new Node("host-02", 9090), "/logs/yyy"));
    }

    private void check(@NonNull String hocon, LogPath ... paths) {
        Config config = ConfigFactory.parseString(hocon);

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));

        Assert.assertThat(resolver.resolvePath("path"), is(Arrays.asList(paths)));

    }
}
