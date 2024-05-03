package com.logviewer.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DelegateProxyTest {

    @Test
    public void invokeMethod() {
        TestInterface itf = DelegateProxy.create(TestInterface.class, new TestClass());
        assertEquals("s", itf.getS());
    }

    @Test
    public void invokeObjectMethods() {
        TestClass delegate = new TestClass();
        TestInterface itf = DelegateProxy.create(TestInterface.class, delegate);
        assertEquals(delegate.hashCode(), itf.hashCode());
        assertEquals(delegate.toString(), itf.toString());
    }

    @Test
    public void testGetDelegate() {
        TestClass delegate = new TestClass();
        TestInterface itf = DelegateProxy.create(TestInterface.class, delegate);
        assertSame(delegate, DelegateProxy.getDelegate(itf));
    }

    public interface TestInterface {

        String getS();

    }

    public static class TestClass {
        public String getS() {
            return "s";
        }
    }
}