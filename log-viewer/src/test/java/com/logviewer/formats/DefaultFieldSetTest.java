package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.*;
import com.logviewer.formats.utils.*;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DefaultFieldSetTest extends AbstractLogTest {

    public static final DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
            new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
            new LvLayoutTextNode(" ["),
            LvLayoutStretchNode.threadNode(),
            new LvLayoutTextNode("] "),
            new LvLayoutFixedTextNode("level", FieldTypes.LEVEL_LOGBACK, "ERROR", "WARN", "INFO", "DEBUG", "TRACE"),
            new LvLayoutClassNode(),
            new LvLayoutTextNode(" - "),
            LvLayoutStretchNode.messageNode());

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

    @Test
    public void testFinalStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                LvLayoutStretchNode.threadNode());

        LogReader reader = format.createReader();

        String s = "2016-12-02_16:05:11.333 localhost-startStop-1";

        assertTrue(reader.parseRecord(new BufferedFile.Line(s)));

        LogRecord record = reader.buildRecord();

        assertEquals("localhost-startStop-1", fieldValue(format, record, "thread"));
    }

    @Test
    public void testStretchPropertyMinSizeAtEnd() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f", "f", false, 3));

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333 ");
        buildFailed(format, "2016-12-02_16:05:11.333  ");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333   ");
        assertEquals("   ", fieldValue(format, record, "f"));

        record = buildRecord(format, "2016-12-02_16:05:11.333      ");
        assertEquals("      ", fieldValue(format, record, "f"));
    }

    @Test
    public void testDoubleStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f1", "f", false, 3),
                new LvLayoutStretchNode("f2", "f", false, 3));

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333...,,");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333...,,,");
        assertEquals("...", fieldValue(format, record, "f1"));
        assertEquals(",,,", fieldValue(format, record, "f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333...,,,__");
        assertEquals("...", fieldValue(format, record, "f1"));
        assertEquals(",,,__", fieldValue(format, record, "f2"));
    }

    @Test
    public void testDoubleStretchPropertyRollback() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f1", "f", false, 1),
                new LvLayoutSimpleDateNode("="),
                new LvLayoutStretchNode("f2", "f", false, 1),
                new LvLayoutSimpleDateNode("=")
        );

        buildFailed(format, "2016-12-02_16:05:11.333.....=_____=,");
        buildFailed(format, "2016-12-02_16:05:11.333.....==");
        buildFailed(format, "2016-12-02_16:05:11.333=,=");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333.....=_____=");
        assertEquals(".....", fieldValue(format, record, "f1"));
        assertEquals("_____", fieldValue(format, record, "f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333.....=_____='''=;;;;;=");
        assertEquals(".....", fieldValue(format, record, "f1"));
        assertEquals("_____='''=;;;;;", fieldValue(format, record, "f2"));
    }

    @Test
    public void testDoubleStretchPropertyRollbackNonSearchabeField() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f1", "f", false, 1),
                new LvLayoutSimpleDateNode("yyyy-MM-dd"),
                new LvLayoutStretchNode("f2", "f", false, 1),
                new LvLayoutSimpleDateNode("yyyy-MM-dd")
                );

        buildFailed(format, "2016-12-02_16:05:11.333.....2016-12-02_____2016-12-02,");
        buildFailed(format, "2016-12-02_16:05:11.333.....2016-12-022016-12-02");
        buildFailed(format, "2016-12-02_16:05:11.3332016-12-02,2016-12-02");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333.....2016-12-02_____2016-12-02");
        assertEquals(".....", fieldValue(format, record, "f1"));
        assertEquals("_____", fieldValue(format, record, "f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333.....2016-12-02_____2016-12-02'''2016-12-02;;;;;2016-12-02");
        assertEquals(".....", fieldValue(format, record, "f1"));
        assertEquals("_____2016-12-02'''2016-12-02;;;;;", fieldValue(format, record, "f2"));
    }
    
    @Test
    public void testEmptyStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                LvLayoutStretchNode.messageNode());

        buildFailed(format, "2016-12-02_16:05:11.333");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 ");
        assertEquals("", fieldValue(format, record, "msg"));
    }

    @Test
    public void testRequiredSpace() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                new LvLayoutClassNode());

        buildFailed(format, "2016-12-02_16:05:11.333com.google.App");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 com.google.App");
        assertEquals("com.google.App", fieldValue(format, record, "logger"));

        record = buildRecord(format, "2016-12-02_16:05:11.333     com.google.App");
        assertEquals("com.google.App", fieldValue(format, record, "logger"));
    }

    @Test
    public void testRegexFieldAfterStretchField() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                new LvLayoutRegexNode("f0", "f", "\\d+"),
                new LvLayoutTextNode(" "),
                LvLayoutStretchNode.threadNode(),
                new LvLayoutRegexNode("f", "f", "\\d+")
                );

        buildFailed(format, "2016-12-02_16:05:11.333 --");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 999 tt555");
        assertEquals("999", fieldValue(format, record, "f0"));
        assertEquals("tt", fieldValue(format, record, "thread"));
        assertEquals("555", fieldValue(format, record, "f"));
    }

    @Test
    public void testEmptyStretchPropertyMiddle() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                LvLayoutStretchNode.messageNode(),
                new LvLayoutClassNode()
                );

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 com.behavox.App");
        assertEquals("", fieldValue(format, record, "msg"));
        assertEquals("com.behavox.App", fieldValue(format, record, "logger"));
    }

    @Test
    public void testStretchProperty5() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode(" "),
                LvLayoutStretchNode.messageNode(),
                new LvLayoutClassNode()
        );

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333 ...");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 mmmm com.google.MyApp");
        assertEquals("mmmm", fieldValue(format, record, "msg"));
        assertEquals("com.google.MyApp", fieldValue(format, record, "logger"));
    }

    @Test
    public void testSpaceAtEnd() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true,
                new LvLayoutFixedTextNode("f", "f", "INFO", "WARN"),
                new LvLayoutTextNode(" ")
        );

        buildFailed(format, "INFO");
        buildFailed(format, "INFO.");

        LogRecord record = buildRecord(format, "INFO ");
        assertEquals("INFO", fieldValue(format, record, "f"));

        record = buildRecord(format, "INFO      ");
        assertEquals("INFO", fieldValue(format, record, "f"));
    }

    private static void buildFailed(DefaultFieldSet format, String s) {
        LogReader reader = format.createReader();
        assertFalse(reader.parseRecord(new BufferedFile.Line(s)));
    }

    private static LogRecord buildRecord(DefaultFieldSet format, String s) {
        LogReader reader = format.createReader();

        assertTrue(reader.parseRecord(new BufferedFile.Line(s)));

        return reader.buildRecord();
    }

    @Test
    public void testStringEnd() {
        DefaultFieldSet format = new DefaultFieldSet(StandardCharsets.UTF_8, true, new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutTextNode("___"), new LvLayoutClassNode());

        LogReader reader = format.createReader();

        assertFalse(reader.parseRecord(new BufferedFile.Line("2016-12-02_16:05:11.333")));
        assertFalse(reader.hasParsedRecord());

        assertFalse(reader.parseRecord(new BufferedFile.Line("2016-12-02_16:05:11.333_")));
        assertFalse(reader.hasParsedRecord());

        assertFalse(reader.parseRecord(new BufferedFile.Line("2016-12-02_16:05:11.333___")));
        assertFalse(reader.hasParsedRecord());

        assertFalse(reader.parseRecord(new BufferedFile.Line("2016-12-02_16:05:11.333___*")));
        assertFalse(reader.hasParsedRecord());

        assertTrue(reader.parseRecord(new BufferedFile.Line("2016-12-02_16:05:11.333___Main")));
        LogRecord record = reader.buildRecord();

        assertEquals("Main", fieldValue(format, record, "logger"));
    }

    @Test
    public void testDefaultFormat() {
        assertEquals(Arrays.asList("date", "thread", "level", "logger", "msg"), Stream.of(format.getFields())
                .map(LogFormat.FieldDescriptor::name)
                .collect(Collectors.toList()));

        LogReader reader = format.createReader();

        String s = "2016-12-02_16:05:11.333 [localhost-startStop-1] INFO  com.behavox.core.PluginManager - Plugins search time: 175 ms";

        byte[] line = ("zzz\n" + s).getBytes();

        line = Arrays.copyOf(line, line.length + 5);

        boolean b = reader.parseRecord(line, 4, s.length(), 10000, 1000100);
        assertTrue(b);

        LogRecord record = reader.buildRecord();

        assertEquals(s, record.getMessage());
        assertEquals("2016-12-02_16:05:11.333", fieldValue(format, record, "date"));
        assertEquals("localhost-startStop-1", fieldValue(format, record, "thread"));
        assertEquals("INFO", fieldValue(format, record, "level"));
        assertEquals("com.behavox.core.PluginManager", fieldValue(format, record, "logger"));
        assertEquals("Plugins search time: 175 ms", fieldValue(format, record, "msg"));

        assertEquals("2016-12-02_16:05:11.333", new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date(record.getTimeMillis())));
    }

    private String fieldValue(DefaultFieldSet format, LogRecord record, String fieldName) {
        int fieldIdx = -1;

        LogFormat.FieldDescriptor[] fields = format.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].name().equals(fieldName)) {
                fieldIdx = i;
                break;
            }
        }

        assert fieldIdx >= 0 : fieldName;
        return record.getFieldText(fieldIdx);
    }

}
