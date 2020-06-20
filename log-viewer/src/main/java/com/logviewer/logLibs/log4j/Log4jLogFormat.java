package com.logviewer.logLibs.log4j;

import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.AbstractPatternLogFormat;
import com.logviewer.formats.utils.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.*;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Log4jLogFormat extends AbstractPatternLogFormat {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("" +
            "(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" +
            "\\." +
            "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" + // method name
            "\\([^)]*\\)"
    );

    public Log4jLogFormat(@Nonnull String pattern) {
        super(null, pattern);
    }

    public Log4jLogFormat(@Nonnull Charset charset, @Nonnull String pattern) {
        super(charset, pattern);
    }

    @Override
    protected LvLayoutNode[] parseLayout(@Nonnull String pattern) throws IllegalArgumentException {
        PatternParser parser = new PatternParser(PatternLayout.KEY);
        List<PatternFormatter> list = new ArrayList<>(parser.parse(pattern));

        if (list.size() > 0) { // Remove last line separator
            LogEventPatternConverter lastItem = list.get(list.size() - 1).getConverter();
            if (lastItem instanceof LineSeparatorPatternConverter)
                list.remove(list.size() - 1);
        }

        List<LvLayoutNode> nodes = new ArrayList<>();

        for (PatternFormatter formatter : list) {
            addNodes(nodes, formatter.getConverter());
        }

        mergeMessageFields(nodes);

        return nodes.toArray(new LvLayoutNode[0]);
    }

    private void addNodes(List<LvLayoutNode> nodes, LogEventPatternConverter converter) {
        LvLayoutNode node = toNode(converter);
        if (node != null) {
            nodes.add(node);
            return;
        }

        if (converter instanceof NdcPatternConverter) {
            nodes.add(new LvLayoutTextNode("["));
            nodes.add(new LvLayoutStretchNode("ndc", FieldTypes.NDC, false, 0));
            nodes.add(new LvLayoutTextNode("]"));
            return;
        }

        if (converter instanceof MdcPatternConverter) {
            node = new LvLayoutStretchNode("mdc", FieldTypes.MDC, true, 0);

            if (hasBrackets((MdcPatternConverter) converter)) {
                nodes.add(new LvLayoutTextNode("{"));
                nodes.add(node);
                nodes.add(new LvLayoutTextNode("}"));
            } else {
                nodes.add(node);
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported pattern: %" + converter.getStyleClass(null));
    }

    private boolean hasBrackets(MdcPatternConverter converter) {
        StringBuilder sb = new StringBuilder();
        converter.format(Log4jLogEvent.newBuilder().build(), sb);

        return sb.toString().equals("{}");
    }

    private LvLayoutNode toNode(LogEventPatternConverter converter) {
        if (converter instanceof LiteralPatternConverter) {
            String literal = ((LiteralPatternConverter) converter).getLiteral();
            return new LvLayoutTextNode(literal);
        }

        if (converter instanceof LevelPatternConverter) {
            return new LvLayoutFixedTextNode("level", FieldTypes.LEVEL_LOG4J,
                    Stream.of(Level.values()).map(Level::name).toArray(String[]::new));
        }

        if (converter instanceof DatePatternConverter) {
            String pattern = ((DatePatternConverter) converter).getPattern();

            if (FixedDateFormat.FixedFormat.ISO8601.getPattern().equals(pattern))
                return new LvLayoutLog4jISO8601Date(); // to compatible log4J_1 and log4J_2

            return new LvLayoutSimpleDateNode(pattern);
        }

        if (converter instanceof ThreadNamePatternConverter) {
            return LvLayoutStretchNode.threadNode();
        }

        if (converter instanceof LoggerPatternConverter || converter instanceof LoggerFqcnPatternConverter
                || converter instanceof ClassNamePatternConverter) {
            return new LvLayoutClassNode();
        }

        if (converter instanceof MessagePatternConverter || converter instanceof ThrowablePatternConverter
                || converter instanceof MapPatternConverter || converter instanceof MarkerPatternConverter
                || converter instanceof MarkerSimpleNamePatternConverter
        ) {
            return LvLayoutStretchNode.messageNode();
        }

        if (converter instanceof FileLocationPatternConverter) {
            return new LvLayoutRegexpNode("sourceFile", "sourceFile", SOURCE_FILE_PATTERN);
        }

        if (converter instanceof LineSeparatorPatternConverter)
            throw new IllegalArgumentException("Unsupported pattern: '%n' can be at the end only");

        if (converter instanceof LineLocationPatternConverter) {
            return new LvLayoutNumberNode("line", null, true);
        }

        if (converter instanceof FullLocationPatternConverter) {
            return new LvLayoutRegexpNode("location", FieldTypes.MESSAGE, LOCATION_PATTERN);
        }

        if (converter instanceof MethodLocationPatternConverter) {
            return new LvLayoutRegexpNode("method", null, METHOD_PATTERN);
        }

        if (converter instanceof NanoTimePatternConverter) {
            return new LvLayoutNumberNode("nano", null);
        }

        if (converter instanceof ProcessIdPatternConverter) {
            return new LvLayoutNumberNode("pid", null);
        }

        if (converter instanceof RelativeTimePatternConverter) {
            return new LvLayoutNumberNode("relative", FieldTypes.RELATIVE_TIMESTAMP);
        }

        if (converter instanceof SequenceNumberPatternConverter) {
            return new LvLayoutNumberNode("sn", null);
        }

        if (converter instanceof ThreadIdPatternConverter) {
            return new LvLayoutNumberNode("tid", null);
        }

        if (converter instanceof ThreadPriorityPatternConverter) {
            return new LvLayoutNumberNode("threadPriority", null);
        }

        if (converter instanceof EndOfBatchPatternConverter) {
            return new LvLayoutFixedTextNode("endOfBatch", null, "true", "false");
        }

        if (converter instanceof UuidPatternConverter) {
            return new LvLayoutRegexpNode("uuid", null, Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
        }

        return null;
    }
}
