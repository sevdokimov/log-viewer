package com.logviewer.formats.utils;

import org.junit.Test;

public class LvLayoutNumberNodeTest extends LayoutNodeTestBase {

    private boolean canBeNegative;
    private boolean canHaveFraction;

    @Test
    public void positiveInteger() {
        check(" aa<pos>123a", "123");
        check(" aa<pos>-123a", null);
        check(" aa<pos>a", null);
        check(" aa<pos>1a", "1");
        check(" aa<pos>1", "1");
        check(" aa<pos>1.", "1");
        check(" aa<pos>1.0", "1");
    }

    @Test
    public void negativeInteger() {
        canBeNegative = true;

        check(" aa<pos>123a", "123");
        check(" aa<pos>-123a", "-123");
        check(" aa<pos>-a", null);
        check(" aa<pos>a", null);
        check(" aa<pos>1", "1");
        check(" aa<pos>-1", "-1");
        check(" aa<pos>-1.", "-1");
        check(" aa<pos>-1.02", "-1");
    }

    @Test
    public void negativeFloat() {
        canBeNegative = true;
        canHaveFraction = true;

        check(" aa<pos>123a", "123");
        check(" aa<pos>-123a", "-123");
        check(" aa<pos>123.a", "123");
        check(" aa<pos>-123.0", "-123.0");
        check(" aa<pos>-123.", "-123");
        check(" aa<pos>3.00001", "3.00001");
        check(" aa<pos>3.00001aaa", "3.00001");
        check(" aa<pos>3.0", "3.0");
    }

    @Override
    protected LvLayoutNode create() {
        return new LvLayoutNumberNode("f", "t", canBeNegative, canHaveFraction);
    }
}