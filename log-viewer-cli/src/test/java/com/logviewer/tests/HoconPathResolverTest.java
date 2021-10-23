package com.logviewer.tests;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.logviewer.AbstractLogTest;
import com.logviewer.HoconPathResolver;
import com.logviewer.TestUtils;
import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.net.Node;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HoconPathResolverTest extends AbstractLogTest {

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

        check("log-paths: {path = {" +
                        "file: \"/logs/xxx\"\n" +
                        "host: []" +
                        "}\n}", new LogPath[0]);

        check("log-paths: {path = {" +
                        "file: []\n" +
                        "}\n}", new LogPath[0]);
    }

    @Test
    public void existingPath() {
        Config config = ConfigFactory.parseString("log-paths: {\n" +
                "path = {\n" +
                "file: \"/logs/xxx.log\"" +
                "}\n}");

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));
        assertNull(resolver.resolvePath("zzzzz"));
    }

    @Test
    public void invalidPatternWildcardDir() {
        Config config = ConfigFactory.parseString("log-paths: {\n" +
                "path = {\n" +
                "file: \"/logs/*/xxx.log\"" +
                "}\n}", ConfigParseOptions.defaults().setOriginDescription("config.conf"));

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> new HoconPathResolver(config.getObject("log-paths")));
        assertThat(e.getMessage(), CoreMatchers.containsString("config.conf: 2"));
        assertThat(e.getMessage(), CoreMatchers.containsString("/logs/*/xxx.log"));
    }

    @Test
    public void invalidPatternNonAbsoluteDir() {
        Config config = ConfigFactory.parseString("log-paths: {\n" +
                "path = {\n" +
                "file: \"logs/*/xxx.log\"" +
                "}\n}", ConfigParseOptions.defaults().setOriginDescription("config.conf"));

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> new HoconPathResolver(config.getObject("log-paths")));
        assertThat(e.getMessage(), CoreMatchers.containsString("config.conf: 2"));
        assertThat(e.getMessage(), CoreMatchers.containsString("logs/*/xxx.log"));
    }

    private Path logDir() {
        return Paths.get(Resources.getResource("path-resolver/aaa-1.log").getFile()).getParent();
    }

    @Test
    public void wildcard() {
        check("log-paths: {\n" +
                        "path = {\n" +
                        "file: \"" + logDir() + "/aaa-*\"" +
                        "}\n}",
                "aaa-1.log", "AAA-2.log");

        check("log-paths: {\n" +
                        "path = {\n" +
                        "file: \"" + logDir() + "/*-1.log\"" +
                        "}\n}",
                "aaa-1.log", "zzz-1.log");

    }

    @Test
    public void wildcardSecurity() {
        Path dir = logDir();

        Config config = ConfigFactory.parseString("log-paths: {\n" +
                "path = {\n" +
                "file: \"" + dir + "/aaa-*\"" +
                "}\n}");

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));

        LvFileAccessManagerImpl accessManager = new LvFileAccessManagerImpl(Collections.emptyList());

        TestUtils.injectDependencies(resolver, accessManager);

        assertThat(resolver.resolvePath("path"), Matchers.empty());

        accessManager.setVisibleFiles(Collections.singletonList(dir.resolve("aaa-1.log")));

        assertThat(resolver.resolvePath("path"),
                is(Collections.singletonList(new LogPath(null, dir.toString() + "/aaa-1.log"))));
    }

    @Test
    public void wildcardDynamic() throws IOException {
        Path dir = Files.createTempDirectory("log-test-");

        try {
            Config config = ConfigFactory.parseString("log-paths: {\n" +
                    "path = {\n" +
                    "file: \"" + dir + "/foo/*.log\"" +
                    "}\n}");

            HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));
            TestUtils.injectDependencies(resolver, new LvFileAccessManagerImpl(null));

            Collection<LogPath> paths = resolver.resolvePath("path");

            assertThat(paths, empty());

            Files.createDirectory(dir.resolve("foo"));

            paths = resolver.resolvePath("path");
            assertThat(paths, empty());

            Path file1 = Files.createFile(dir.resolve("foo/a.log"));

            paths = resolver.resolvePath("path");
            assertEquals(paths, Collections.singletonList(new LogPath(null, file1.toString())));
        }
        finally {
            FileSystemUtils.deleteRecursively(dir.toFile());
        }
    }

    @Test
    public void wildcardRemote() {
        ApplicationContext context = getCommonContext();

        ConfigurableApplicationContext remoteContext = createContext(LvTestConfig.class, LogViewerServerConfig.class);
        LvFileAccessManagerImpl accessManager = remoteContext.getBean(LvFileAccessManagerImpl.class);

        Path logFilePath = logDir().resolve("aaa-1.log");

        accessManager.setVisibleFiles(Collections.singletonList(logFilePath));

        Config config = ConfigFactory.parseString("log-paths: {\n" +
                "path = {\n" +
                "  host: localhost\n" +
                "  port: " + TEST_SERVER_PORT + "\n" +
                "  file: \"" + logDir() + "/*.log\"" +
                "}\n}");

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));
        context.getAutowireCapableBeanFactory().autowireBean(resolver);

        Collection<LogPath> res = resolver.resolvePath("path");

        LogPath expectedPath = new LogPath(new Node("localhost", TEST_SERVER_PORT), logFilePath.toString());
        assertEquals(Collections.singletonList(expectedPath), res );
    }

    private void check(@NonNull String hocon, String ... paths) {
        Config config = ConfigFactory.parseString(hocon);

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));
        TestUtils.injectDependencies(resolver, new LvFileAccessManagerImpl(null));

        Collection<LogPath> res = resolver.resolvePath("path");
        assert res != null;
        Set<String> fileNames = res.stream()
                .map(p -> {
                    Path path = Paths.get(p.getFile());
                    assert path.isAbsolute();
                    return path.getFileName().toString();
                })
                .collect(Collectors.toSet());

        assertEquals(Sets.newHashSet(paths), fileNames);
    }

    private void check(@NonNull String hocon, LogPath ... paths) {
        Config config = ConfigFactory.parseString(hocon);

        HoconPathResolver resolver = new HoconPathResolver(config.getObject("log-paths"));

        assertThat(resolver.resolvePath("path"), is(Arrays.asList(paths)));
    }
}
