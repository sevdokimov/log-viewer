package com.logviewer;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class ConfigTest extends AbstractLogTest {

//    @Test
//    public void testConfigReload() throws IOException, InterruptedException {
//        Path tempFile = Files.createTempFile("testCfg-", ".json");
//
//        try {
//            Files.write(tempFile, "{}".getBytes());
//
//            Properties props = new Properties();
//            props.setProperty(FileConfig.CONFIG_FILE_PROPERTY, tempFile.toAbsolutePath().toString());
//
//            TestConfigDirHolder dirHolder = new TestConfigDirHolder(environment(props));
//
//            try (FileConfig fileConfig = new FileConfig(dirHolder)) {
//                fileConfig.init();
//
//                assertEquals(1, fileConfig.getReloadCount());
//
//                Files.write(tempFile, "{\"disableUnknownFiles\": true}".getBytes());
//
//                Thread.sleep(50);
//
//                assertEquals(2, fileConfig.getReloadCount());
//            } finally {
//                dirHolder.destroy();
//            }
//        }
//        finally {
//            Files.delete(tempFile);
//        }
//    }


    @Test
    public void name() throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(
                Files.newInputStream(Paths.get("/home/sevdokimov/ttt/file.tar.gz")))) {
            ByteStreams.copy(gis, System.out);
        }
    }

    private static Environment environment(Properties properties) {
        return new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addFirst(new PropertiesPropertySource("props", properties));
            }
        };
    }

//    @Test
//    public void testStartOnNewSystem() throws IOException, InterruptedException {
//        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FileConfigConfig.class)) {
//            try (FileConfig fileConfig = ctx.getBean(FileConfig.class)) {
//
//                Files.write(fileConfig.getConfigFile(), "{\"disableUnknownFiles\": true}".getBytes());
//
//                Thread.sleep(50);
//            }
//        }
//    }

//    @Import(LvTestConfig.class)
//    @Configuration
//    public static class FileConfigConfig {
//        @Bean
//        public Config lvConfig(ConfigDirHolder configDir) {
//            return new FileConfig(configDir);
//        }
//    }
}