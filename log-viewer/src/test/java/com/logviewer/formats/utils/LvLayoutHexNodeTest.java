package com.logviewer.formats.utils;

import org.junit.Test;

public class LvLayoutHexNodeTest extends LayoutNodeTestBase {

    @Test
    public void test() {
        check(" aa<pos>123a", "123a");
        check(" aa<pos>z", null);
        check(" aa<pos>-123a", null);
        check(" aa<pos>abcdefgh", "abcdef");
        check(" aa<pos>AbcDEfgh", "AbcDEf");
        check(" aa<pos>A12bcDE2", "A12bcDE2");
    }

    @Override
    protected LvLayoutNode create() {
        return new LvLayoutHexNode("f", "t", 2);
    }
}