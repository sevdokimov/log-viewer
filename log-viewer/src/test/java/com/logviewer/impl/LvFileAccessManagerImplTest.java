package com.logviewer.impl;

import com.logviewer.TestUtils;
import com.logviewer.api.LvFileAccessManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LvFileAccessManagerImplTest {

    @Test
    public void testNullHoconConfig() {
        LvFileAccessManager accessManager = new LvFileAccessManagerImpl((Config) null);
        assertNull(accessManager.checkAccess(Paths.get("/tmp/a.log")));
    }

    @Test
    public void testNoHoconConfig() {
        LvFileAccessManager accessManager = new LvFileAccessManagerImpl(ConfigFactory.parseString("{}"));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/a.log")));
    }

    @Test
    public void testHoconConfigStr() {
        Config config = ConfigFactory.parseString("{" +
                "visible-directories: [\"/tmp/zzz\"]" +
                "}");

        LvFileAccessManager accessManager = new LvFileAccessManagerImpl(config);
        assertNull(accessManager.checkAccess(Paths.get("/")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz/a.log")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz/subdir/a.log")));

        assertNotNull(accessManager.checkAccess(Paths.get("/tmp/b.log")));
    }

    @Test
    public void testHoconConfig2() {
        Config config = ConfigFactory.parseString("{" +
                "visible-directories: [{directory: \"/tmp/zzz\", regexp: \".+\\\\.log\"}]" +
                "}");

        LvFileAccessManager accessManager = new LvFileAccessManagerImpl(config);
        assertNull(accessManager.checkAccess(Paths.get("/")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz/a.log")));
        assertNull(accessManager.checkAccess(Paths.get("/tmp/zzz/subdir/a.log")));

        assertNotNull(accessManager.checkAccess(Paths.get("/tmp/b.log")));
        assertNotNull(accessManager.checkAccess(Paths.get("/tmp/zzz/b.txt")));
        assertNotNull(accessManager.checkAccess(Paths.get("/tmp/zzz/subdir/b.txt")));
    }

    @Test
    public void testInvalidHocon() {
        Config config = ConfigFactory.parseString("{" +
                "visible-directories: [{d: \"/tmp/zzz\", r: \".+\\\\.log\"}]" +
                "}");

        TestUtils.assertError(IllegalArgumentException.class, () -> {
            new LvFileAccessManagerImpl(config);
        });
    }

    @Test
    public void testInvalidHocon2() {
        Config config = ConfigFactory.parseString("{visible-directories: [55]}");

        TestUtils.assertError(IllegalArgumentException.class, () -> {
            new LvFileAccessManagerImpl(config);
        });
    }

}
