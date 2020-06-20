package com.logviewer;

import com.google.common.collect.Sets;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.api.LvPathResolver;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.LogPath;
import com.logviewer.web.dto.events.EventScrollToEdgeResponse;
import com.logviewer.web.session.LogSession;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CustomPathResolverTest extends LogSessionTestBase {

    private static Path tempDir;

    @Test
    public void testCustomParamParser() throws InterruptedException, IOException {
        tempDir = createTempDirectory();

        Files.write(tempDir.resolve("a.txt"), new byte[0]);

        ApplicationContext ctx = createContext(MyConfig.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(new String[]{"abc", getTestLog("multilog/server-a.log")}, "default", null);
        session.scrollToEdge(3, 2, null, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);

        assertEquals(Sets.newHashSet("a.txt", "b.txt", "server-a.log"), init.statuses.keySet());
        assertEquals(0, init.statuses.get("a.txt").getSize());
        assertNull(init.statuses.get("a.txt").getErrorType());

        assertEquals("NoSuchFileException", init.statuses.get("b.txt").getErrorType());

        assertNull(init.statuses.get("server-a.log").getErrorType());
        assert init.statuses.get("server-a.log").getSize() > 0;
    }

    @Configuration
    public static class MyConfig extends LvTestConfig {
        @Bean
        public LvPathResolver pathResolver() {
            return new LvPathResolver() {
                @Nullable
                @Override
                public Collection<LogPath> resolvePath(@Nonnull String pathFromHttpParameter) {
                    if (pathFromHttpParameter.equals("abc")) {
                        return Arrays.asList(new LogPath(null, tempDir.resolve("a.txt").toString()),
                                new LogPath(null, tempDir.resolve("b.txt").toString()));
                    }

                    return null;
                }
            };
        }

        @Bean
        public LvFormatRecognizer formatRecognizer() {
            return path -> TestUtils.MULTIFILE_LOG_FORMAT;
        }
    }

}
