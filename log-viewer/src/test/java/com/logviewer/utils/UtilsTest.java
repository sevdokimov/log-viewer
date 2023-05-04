package com.logviewer.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void testFileComparision() {
        assert Utils.compareFileNames("abc", "") > 0;
        assert Utils.compareFileNames("abc", "abc") == 0;
        assert Utils.compareFileNames("abc", "abC") == 0;
        assert Utils.compareFileNames("abc", "a") > 0;
        assert Utils.compareFileNames("a10c", "a9c") > 0;
        assert Utils.compareFileNames("a1c", "a9c") < 0;
        assert Utils.compareFileNames("a100000000000000000000000000000000000c", "a9c") > 0;
        assert Utils.compareFileNames("a10b20cc", "a10b20d") < 0;
        assert Utils.compareFileNames("a10b20cc", "a10b20") > 0;
        assert Utils.compareFileNames("a10b20c", "a10b20C") == 0;
        assert Utils.compareFileNames("a10b20c10", "a10b20c3") > 0;
    }

    @Test
    public void testRemoveAsciiColorCodes() {
        assertEquals("", Utils.removeAsciiColorCodes(""));
        assertEquals("aaa", Utils.removeAsciiColorCodes("aaa"));
        assertEquals("aaa", Utils.removeAsciiColorCodes("aaa"));
        assertEquals("\u001B", Utils.removeAsciiColorCodes("\u001B"));
        assertEquals("\u001Ba", Utils.removeAsciiColorCodes("\u001Ba"));
        assertEquals("aa\u001B", Utils.removeAsciiColorCodes("aa\u001B"));
        assertEquals("aa\u001Bxx", Utils.removeAsciiColorCodes("aa\u001Bxx"));
        assertEquals("aa\u001B[", Utils.removeAsciiColorCodes("aa\u001B["));
        assertEquals("aa\u001B[xx", Utils.removeAsciiColorCodes("aa\u001B[xx"));
        assertEquals("aa\u001B[1;z", Utils.removeAsciiColorCodes("aa\u001B[1;z"));
        assertEquals("aa\u001B[1", Utils.removeAsciiColorCodes("aa\u001B[1"));
        assertEquals("aa", Utils.removeAsciiColorCodes("aa\u001B[1m"));
        assertEquals("aa", Utils.removeAsciiColorCodes("\u001B[2;51maa\u001B[1m"));
        assertEquals("aa!", Utils.removeAsciiColorCodes("aa\u001B[1m!"));
        assertEquals("aa!", Utils.removeAsciiColorCodes("aa\u001B[m!"));
        assertEquals("aa!", Utils.removeAsciiColorCodes("aa\u001B[1;1m!"));
        assertEquals("aa!", Utils.removeAsciiColorCodes("aa\u001B[1;1;44m!"));
        assertEquals("aa\u001B[11;22K!", Utils.removeAsciiColorCodes("aa\u001B[11;22K!"));
        assertEquals("aa\u001B[11;22K!_fff", Utils.removeAsciiColorCodes("aa\u001B[11;22K!\u001B[11m\u001B[1;1m_\u001B[1;1mfff"));
    }

}
