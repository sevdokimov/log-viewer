package com.logviewer.utils;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class PersistnetMapTest {

    @Test
    public void testPersistentMap() throws IOException {
        Path file = Files.createTempFile("tmp", ".data");

        try {
            PersistentMap map = new PersistentMap(file, 1000);

            map.put("z", "1");
            map.put("x", "2");
            map.put("f", "3");

            PersistentMap map2 = new PersistentMap(file, 1000);

            assert map2.getMemoryUsage() == 0;

            assertEquals("1", map2.get("z"));
            assertEquals("2", map2.get("x"));
            assertEquals("3", map2.get("f"));

        } finally {
            Files.delete(file);
        }
    }

}
