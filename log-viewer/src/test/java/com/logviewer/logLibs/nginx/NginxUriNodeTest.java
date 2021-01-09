package com.logviewer.logLibs.nginx;

import com.logviewer.formats.utils.LayoutNodeTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NginxUriNodeTest extends LayoutNodeTestBase {

    @Test
    public void testUnexpectedSymbols() {
        NginxUriNode node = create();

        String s = " aa_;.&+e\\u2222|";
        if (!node.reset(s, 1, s.length()))
            throw new RuntimeException();
        assertEquals(2, node.getEnd());

        for (int i = 3; i <= 9; i++) {
            node.grow(s, i, s.length());
            assertEquals(i, node.getEnd());
        }

        node.grow(s, 10, s.length());
        assertEquals(15, node.getEnd());

        assert !node.grow(s, 16, s.length());
    }

    @Override
    protected NginxUriNode create() {
        return new NginxUriNode("aaa");
    }
}