package com.logviewer.utils;

import com.logviewer.data2.FieldTypes;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FieldTypesTest {

    @Test
    public void testId() {
        assertFalse(FieldTypes.is(null, FieldTypes.DATE));
        assertFalse(FieldTypes.is(null, FieldTypes.LEVEL_LOGBACK));

        assertTrue(FieldTypes.is(FieldTypes.DATE, FieldTypes.DATE));
        assertTrue(FieldTypes.is(FieldTypes.LEVEL_LOGBACK, FieldTypes.LEVEL_LOGBACK));
        assertFalse(FieldTypes.is("level", FieldTypes.LEVEL_LOGBACK));
        assertTrue(FieldTypes.is(FieldTypes.LEVEL_LOGBACK, "level"));
        assertFalse(FieldTypes.is(FieldTypes.LEVEL_LOGBACK, "lev"));
        assertFalse(FieldTypes.is(FieldTypes.LEVEL_LOGBACK, "levelx"));
        assertFalse(FieldTypes.is(FieldTypes.LEVEL_LOG4J, FieldTypes.LEVEL_LOGBACK));
    }
}
