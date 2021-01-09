package com.logviewer.logLibs.nginx;

import com.logviewer.formats.utils.LayoutNodeTestBase;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class NginxStretchPatternNodeTest extends LayoutNodeTestBase {

    @Test
    public void reset1() {
        NginxStretchPatternNode node = create();

        String s = "0123456 \\x55";
        if (!node.reset(s, 0, s.length()))
            throw new IllegalStateException();

        assertEquals("0123456", s.substring(node.getStart(), node.getEnd()));

        if (!node.grow(s, 8, s.length()))
            throw new RuntimeException();

        assertEquals("0123456 ", s.substring(node.getStart(), node.getEnd()));


        if (!node.grow(s, 9, s.length()))
            throw new RuntimeException();

        assertEquals("0123456 \\x55", s.substring(node.getStart(), node.getEnd()));
    }

    @Test
    public void reset2() {
        NginxStretchPatternNode node = create();

        String s = "aa0123456 \\x55";
        if (node.reset(s, 0, s.length()))
            throw new IllegalStateException();

        if (!node.reset(s, 2, s.length()))
            throw new IllegalStateException();

        assertEquals("0123456", s.substring(node.getStart(), node.getEnd()));
    }

    @Override
    protected NginxStretchPatternNode create() {
        return new NginxStretchPatternNode("a", "a", true, Pattern.compile("\\d+"));
    }
}