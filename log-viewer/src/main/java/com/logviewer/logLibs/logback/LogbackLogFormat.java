package com.logviewer.logLibs.logback;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.AbstractPatternLogFormat;
import com.logviewer.formats.utils.*;
import com.logviewer.data2.LogLevels;
import org.slf4j.event.Level;
import org.springframework.lang.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LogbackLogFormat extends AbstractPatternLogFormat {

    protected static final int NODE_LITERAL = 0; // Node.LITERAL
    protected static final int NODE_SIMPLE_KEYWORD = 1; // Node.COMPOSITE_KEYWORD
    protected static final int NODE_COMPOSITE_KEYWORD = 2; // Node.COMPOSITE_KEYWORD

    public LogbackLogFormat(@NonNull String pattern) {
        super(pattern);
    }

    @Override
    protected LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException {
        Node t;
        try {
            Parser parser = new Parser(pattern);
            t = parser.parse();
        } catch (ScanException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        List<LvLayoutNode> nodes = new ArrayList<>();

        for (Node n = t; n != null; n = n.getNext()) {
            LvLayoutNode lvNode = createNode(n, pattern);
            if (lvNode != null)
                nodes.add(lvNode);
        }

        mergeMessageFields(nodes);

        return nodes.toArray(new LvLayoutNode[0]);
    }

    private LvLayoutNode createNode(Node n, String pattern) {
        switch (n.getType()) {
            case NODE_LITERAL:
                return LvLayoutTextNode.of((String) n.getValue());
            case NODE_COMPOSITE_KEYWORD:
                throw new IllegalArgumentException("log encoder pattern contains unsupported terms [pattern=\""
                        + pattern + "\", term=" + n + "]");
            case NODE_SIMPLE_KEYWORD:
                SimpleKeywordNode kn = (SimpleKeywordNode) n;
                String keyword = (String) kn.getValue();

                switch (keyword) {
                    case "d":
                    case "date": { // see ch.qos.logback.classic.pattern.DateConverter
                        String datePattern;

                        if (kn.getOptions() == null || kn.getOptions().isEmpty()) {
                            datePattern = CoreConstants.ISO8601_PATTERN;
                        }
                        else {
                            datePattern = kn.getOptions().get(0);
                            if (datePattern.equals(CoreConstants.ISO8601_STR)) {
                                datePattern = CoreConstants.ISO8601_PATTERN;
                            }

                            try {
                                new SimpleDateFormat(datePattern); // validation
                            } catch (IllegalArgumentException e) {
                                datePattern = CoreConstants.ISO8601_PATTERN;
                            }
                        }

                        LvLayoutDateNode res = LvLayoutLog4jISO8601Date.fromPattern(datePattern);  // Optimization, LvLayoutLog4jISO8601Date works much faster.
                        if (res == null)
                            res = new LvLayoutSimpleDateNode(datePattern);

                        return res.withLocale(getLocale());
                    }

                    case "c":
                    case "lo":
                    case "logger":
                    case "C":
                    case "class":
                        return new LvLayoutClassNode();

                    case "F":
                    case "file":
                        return new LvLayoutRegexNode("sourceFile", "sourceFile", SOURCE_FILE_PATTERN);

                    case "t":
                    case "thread":
                        return LvLayoutStretchNode.threadNode();

                    case "relative":
                    case "r":
                        return new LvLayoutNumberNode("relativeTime", FieldTypes.RELATIVE_TIMESTAMP);

                    case "M":
                    case "method":
                        return new LvLayoutRegexNode("method", null, METHOD_PATTERN);

                    case "L":
                    case "line":
                        return new LvLayoutNumberNode("line", null, true);

                    case "processId":
                        return new LvLayoutNumberNode("pid", FieldTypes.PROCESS_ID);

                    case "p":
                    case "le":
                    case "level":
                        return new LvLayoutFixedTextNode("level", FieldTypes.LEVEL_LOGBACK,
                                Stream.concat(
                                        Stream.of(Level.values()).map(Level::toString),
                                        Stream.of(LogLevels.getLevels())
                                ).distinct().toArray(String[]::new));

                    case "nopex":
                    case "nopexception":
                    case "n":
                        return null;

                    case "X":
                    case "mdc":
                        if (!isNextLiteralOrEnd(kn))
                            return LvLayoutStretchNode.messageNode();

                        if (kn.getOptions() == null || kn.getOptions().isEmpty())
                            return new LvLayoutStretchNode("mdc", FieldTypes.MDC, true, 0);

                        String mdcPropertyName = getMdcPropertyName(kn.getOptions().get(0));
                        return new LvLayoutStretchNode(mdcPropertyName, FieldTypes.MDC, true, 0);

                    case "ex":
                    case "exception":
                    case "throwable":
                    case "xEx":
                    case "xException":
                    case "xThrowable":
                    case "wEx":
                    case "wex":
                    case "m":
                    case "msg":
                    case "message":
                        return LvLayoutStretchNode.messageNode();

                    default:
                        throw new IllegalArgumentException("log encoder pattern contains unsupported terms [pattern=\""
                                + pattern + "\", term=" + n + "]");
                }

            default:
                throw new IllegalArgumentException("Unknown node type: " + n.getType() + ", pattern=" + pattern);
        }
    }

    private static String getMdcPropertyName(String s) {
        String[] strings = OptionHelper.extractDefaultReplacement(s);
        return strings[0];
    }

    private static boolean isNextLiteralOrEnd(SimpleKeywordNode kn) {
        Node next = kn.getNext();
        return next == null || next.getType() == NODE_LITERAL;
    }

    @Override
    public String getHumanReadableString() {
        return "logback: " + getPattern();
    }
}
