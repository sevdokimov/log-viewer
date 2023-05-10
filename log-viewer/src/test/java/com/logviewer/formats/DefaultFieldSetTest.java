package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.*;
import com.logviewer.formats.utils.*;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DefaultFieldSetTest extends AbstractLogTest {

    public static final DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
            new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
            LvLayoutTextNode.of(" ["),
            LvLayoutStretchNode.threadNode(),
            LvLayoutTextNode.of("] "),
            new LvLayoutFixedTextNode("level", FieldTypes.LEVEL_LOGBACK, "ERROR", "WARN", "INFO", "DEBUG", "TRACE"),
            new LvLayoutClassNode(),
            LvLayoutTextNode.of(" - "),
            LvLayoutStretchNode.messageNode());

    @Test
    public void testFinalStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                LvLayoutStretchNode.threadNode());

        LogReader reader = format.createReader();

        String s = "2016-12-02_16:05:11.333 localhost-startStop-1";

        assertTrue(reader.parseRecord(new BufferedFile.Line(s)));

        LogRecord record = reader.buildRecord();

        assertEquals("localhost-startStop-1", record.getFieldText("thread"));
    }

    @Test
    public void testStretchPropertyMinSizeAtEnd() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f", "f", false, 3));

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333 ");
        buildFailed(format, "2016-12-02_16:05:11.333  ");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333   ");
        assertEquals("   ", record.getFieldText("f"));

        record = buildRecord(format, "2016-12-02_16:05:11.333      ");
        assertEquals("      ", record.getFieldText("f"));
    }

    @Test
    public void testDoubleStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                new LvLayoutStretchNode("f1", "f", false, 3),
                new LvLayoutStretchNode("f2", "f", false, 3));

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333...,,");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333...,,,");
        assertEquals("...", record.getFieldText("f1"));
        assertEquals(",,,", record.getFieldText("f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333...,,,__");
        assertEquals("...", record.getFieldText("f1"));
        assertEquals(",,,__", record.getFieldText("f2"));
    }

    @Test
    public void testDoubleStretchPropertyRollback() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
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
        assertEquals(".....", record.getFieldText("f1"));
        assertEquals("_____", record.getFieldText("f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333.....=_____='''=;;;;;=");
        assertEquals(".....", record.getFieldText("f1"));
        assertEquals("_____='''=;;;;;", record.getFieldText("f2"));
    }

    @Test
    public void testDoubleStretchPropertyRollbackNonSearchabeField() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
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
        assertEquals(".....", record.getFieldText("f1"));
        assertEquals("_____", record.getFieldText("f2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333.....2016-12-02_____2016-12-02'''2016-12-02;;;;;2016-12-02");
        assertEquals(".....", record.getFieldText("f1"));
        assertEquals("_____2016-12-02'''2016-12-02;;;;;", record.getFieldText("f2"));
    }
    
    @Test
    public void testEmptyStretchProperty() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                LvLayoutStretchNode.messageNode());

        buildFailed(format, "2016-12-02_16:05:11.333");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 ");
        assertEquals("", record.getFieldText("msg"));
    }

    @Test
    public void testRequiredSpace() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                new LvLayoutClassNode());

        buildFailed(format, "2016-12-02_16:05:11.333com.google.App");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 com.google.App");
        assertEquals("com.google.App", record.getFieldText("logger"));

        record = buildRecord(format, "2016-12-02_16:05:11.333     com.google.App");
        assertEquals("com.google.App", record.getFieldText("logger"));
    }

    @Test
    public void testRemovingSpaces() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                new LvLayoutStretchNode("mdc", FieldTypes.MDC, true, 0),
                LvLayoutTextNode.of(" "),
                new LvLayoutStretchNode("mdc2", FieldTypes.MDC, true, 0),
                LvLayoutTextNode.of("!")
        );

        buildFailed(format, "2016-12-02_16:05:11.333 bbb!");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 aaa   bbb!");
        assertEquals("aaa", record.getFieldText("mdc"));
        assertEquals("bbb", record.getFieldText("mdc2"));

        record = buildRecord(format, "2016-12-02_16:05:11.333     aaa   bbb  !");

        record = buildRecord(format, "2016-12-02_16:05:11.333  bbb!");
        assertEquals("", record.getFieldText("mdc"));
        assertEquals("bbb", record.getFieldText("mdc2"));
    }

    @Test
    public void testRegexFieldAfterStretchField() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                new LvLayoutRegexNode("f0", "f", "\\d+"),
                LvLayoutTextNode.of(" "),
                LvLayoutStretchNode.threadNode(),
                new LvLayoutRegexNode("f", "f", "\\d+")
                );

        buildFailed(format, "2016-12-02_16:05:11.333 --");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 999 tt555");
        assertEquals("999", record.getFieldText("f0"));
        assertEquals("tt", record.getFieldText("thread"));
        assertEquals("555", record.getFieldText("f"));
    }

    @Test
    public void testEmptyStretchPropertyMiddle() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                LvLayoutStretchNode.messageNode(),
                new LvLayoutClassNode()
                );

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 com.behavox.App");
        assertEquals("", record.getFieldText("msg"));
        assertEquals("com.behavox.App", record.getFieldText("logger"));
    }

    @Test
    public void testStretchProperty5() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of(" "),
                LvLayoutStretchNode.messageNode(),
                new LvLayoutClassNode()
        );

        buildFailed(format, "2016-12-02_16:05:11.333");
        buildFailed(format, "2016-12-02_16:05:11.333 ...");

        LogRecord record = buildRecord(format, "2016-12-02_16:05:11.333 mmmm com.google.MyApp");
        assertEquals("mmmm", record.getFieldText("msg"));
        assertEquals("com.google.MyApp", record.getFieldText("logger"));
    }

    @Test
    public void testSpaceAtEnd() {
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true,
                new LvLayoutFixedTextNode("f", "f", "INFO", "WARN"),
                LvLayoutTextNode.of(" ")
        );

        buildFailed(format, "INFO");
        buildFailed(format, "INFO.");

        LogRecord record = buildRecord(format, "INFO ");
        assertEquals("INFO", record.getFieldText("f"));

        record = buildRecord(format, "INFO      ");
        assertEquals("INFO", record.getFieldText("f"));
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
        DefaultFieldSet format = new DefaultFieldSet(Locale.US, StandardCharsets.UTF_8, true, new LvLayoutSimpleDateNode("yyyy-MM-dd_HH:mm:ss.SSS"),
                LvLayoutTextNode.of("___"), new LvLayoutClassNode());

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

        assertEquals("Main", record.getFieldText("logger"));
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
        assertEquals("2016-12-02_16:05:11.333", record.getFieldText("date"));
        assertEquals("localhost-startStop-1", record.getFieldText("thread"));
        assertEquals("INFO", record.getFieldText("level"));
        assertEquals("com.behavox.core.PluginManager", record.getFieldText("logger"));
        assertEquals("Plugins search time: 175 ms", record.getFieldText("msg"));

        assertEquals("2016-12-02_16:05:11.333", new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date(record.getTimeMillis())));
    }
}
