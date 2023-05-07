package com.logviewer.utils;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    @Test
    public void decodeReplaceCharOnly() {
        Pair<String, Integer> pair = Utils.decode("\uFFFD\uFFFD\uFFFD".getBytes(), StandardCharsets.UTF_8);
        assertEquals("\uFFFD\uFFFD\uFFFD", pair.getFirst());
        assertEquals("\uFFFD\uFFFD\uFFFD".getBytes().length, pair.getSecond().intValue());
    }

    @Test
    public void decodeEmpty() {
        Pair<String, Integer> pair = Utils.decode("".getBytes(), StandardCharsets.UTF_8);
        assertEquals("", pair.getFirst());
        assertEquals(0, pair.getSecond().intValue());
    }

    @Test
    public void decodeReplaceAtBegin() {
        byte[] bytes = "\uFFFDaы".getBytes(StandardCharsets.UTF_8);
        Pair<String, Integer> pair = Utils.decode(Arrays.copyOf(bytes, 3 + 1 + 1), StandardCharsets.UTF_8);
        assertEquals("\uFFFDa", pair.getFirst());
        assertEquals(3 + 1, pair.getSecond().intValue());
    }

    @Test
    public void decodeUtf8() {
        byte[] bytes = "aы".getBytes(StandardCharsets.UTF_8);

        Pair<String, Integer> pair = Utils.decode(Arrays.copyOf(bytes, 2), StandardCharsets.UTF_8);
        assertEquals("a", pair.getFirst());
        assertEquals(1, pair.getSecond().intValue());

        pair = Utils.decode(bytes, StandardCharsets.UTF_8);
        assertEquals("aы", pair.getFirst());
        assertEquals(3, pair.getSecond().intValue());
    }

    @Test
    public void decodeUtf16() {
        byte[] bytes = new byte[]{0, 97, 0, 98}; // "ab".getBytes(StandardCharsets.UTF_16);

        Pair<String, Integer> pair = Utils.decode(Arrays.copyOf(bytes, 2), StandardCharsets.UTF_16);
        assertEquals("a", pair.getFirst());
        assertEquals(2, pair.getSecond().intValue());

        pair = Utils.decode(Arrays.copyOf(bytes, 3), StandardCharsets.UTF_16);
        assertEquals("a", pair.getFirst());
        assertEquals(2, pair.getSecond().intValue());

        pair = Utils.decode(bytes, StandardCharsets.UTF_16);
        assertEquals("ab", pair.getFirst());
        assertEquals(4, pair.getSecond().intValue());
    }

    @Test
    public void decodeUtf32() {
        byte[] bytes = new byte[]{0, 0, 0, 97, 0, 0, 0, 98}; // "ab".getBytes(StandardCharsets.UTF_16);

        Charset charset = Charset.forName("utf-32");

        Pair<String, Integer> pair = Utils.decode(Arrays.copyOf(bytes, 2), charset);
        assertEquals("\uFFFD", pair.getFirst());
        assertEquals(2, pair.getSecond().intValue());

        pair = Utils.decode(Arrays.copyOf(bytes, 4), charset);
        assertEquals("a", pair.getFirst());
        assertEquals(4, pair.getSecond().intValue());

        pair = Utils.decode(Arrays.copyOf(bytes, 5), charset);
        assertEquals("a", pair.getFirst());
        assertEquals(4, pair.getSecond().intValue());

        pair = Utils.decode(Arrays.copyOf(bytes, 7), charset);
        assertEquals("a", pair.getFirst());
        assertEquals(4, pair.getSecond().intValue());

        pair = Utils.decode(bytes, charset);
        assertEquals("ab", pair.getFirst());
        assertEquals(8, pair.getSecond().intValue());
    }
}
