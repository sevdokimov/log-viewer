package com.logviewer.services;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class PathPatternTest {

    @Test
    public void fromDirectory() {
        PathPattern d = PathPattern.directory(Paths.get("/aaa/bbb"));

        assertEquals(Paths.get("/aaa/bbb"), d.getPrefix());

        assertTrue(d.matchFile(Paths.get("/aaa/bbb/l.log")));
        assertTrue(d.matchFile(Paths.get("/aaa/bbb/ccc/ddd/l.log")));
        assertFalse(d.matchFile(Paths.get("/aaa/z.log")));
        assertFalse(d.matchFile(Paths.get("/z.log")));
        assertFalse(d.matchFile(Paths.get("/aaa/ggg/z.log")));

        assertTrue(d.matchDir(Paths.get("/aaa/bbb")));
        assertTrue(d.matchDir(Paths.get("/aaa/bbb/ccc/ddd")));
        assertTrue(d.matchDir(Paths.get("/aaa")));
        assertTrue(d.matchDir(Paths.get("/")));

        assertFalse(d.matchDir(Paths.get("/aaa/zzz")));
        assertFalse(d.matchDir(Paths.get("/aaa2/bbb/")));

    }

    @Test
    public void fromFile() {
        PathPattern d = PathPattern.file(Paths.get("/aaa/bbb/l.log"));
        assertEquals(Paths.get("/aaa/bbb"), d.getPrefix());
        assertTrue(d.matchFile(Paths.get("/aaa/bbb/l.log")));
        assertFalse(d.matchFile(Paths.get("/aaa/l.log")));
        assertFalse(d.matchFile(Paths.get("/aaa/bbb/z.log")));

        assertFalse(d.matchDir(Paths.get("/aaa/bbb/l.log")));
        assertFalse(d.matchDir(Paths.get("/aaa/zzz")));
        assertTrue(d.matchDir(Paths.get("/aaa/bbb")));
        assertTrue(d.matchDir(Paths.get("/aaa")));
        assertTrue(d.matchDir(Paths.get("/")));
    }
}