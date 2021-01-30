package com.logviewer.logLibs.logback;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;
import ch.qos.logback.core.spi.ScanException;
import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.AbstractPatternLogFormat;
import com.logviewer.formats.utils.*;
import org.slf4j.event.Level;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class LogbackLogFormat extends AbstractPatternLogFormat {

    private static final int NODE_LITERAL = 0; // Node.LITERAL
    private static final int NODE_SIMPLE_KEYWORD = 1; // Node.COMPOSITE_KEYWORD
    private static final int NODE_COMPOSITE_KEYWORD = 2; // Node.COMPOSITE_KEYWORD

    private static final Set<String> TEXT_FILEDS = new HashSet<>(Arrays.asList("ex", "exception", "throwable",
            "xEx", "xException", "xThrowable", "X", "mdc", "m", "msg", "message"));

    public LogbackLogFormat(@NonNull String pattern) {
        super(null, pattern);
    }

    public LogbackLogFormat(@Nullable Charset charset, @NonNull String pattern) {
        super(charset, pattern);
    }

    @Override
    protected LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException {
        Node t;
        try {
            Parser parser = new Parser(pattern);
            t = parser.parse();
        } catch (ScanException e) {
            throw new IllegalArgumentException(e);
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

    private static LvLayoutNode createNode(Node n, String pattern) {
        switch (n.getType()) {
            case NODE_LITERAL:
                return new LvLayoutTextNode((String) n.getValue());
            case NODE_COMPOSITE_KEYWORD:
                throw new IllegalArgumentException("log encoder pattern contains unsupported terms [pattern=\""
                        + pattern + "\", term=" + n.toString() + "]");
            case NODE_SIMPLE_KEYWORD:
                SimpleKeywordNode kn = (SimpleKeywordNode) n;
                String keyword = (String) kn.getValue();

                if (TEXT_FILEDS.contains(keyword))
                    return LvLayoutStretchNode.messageNode();

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
                                new SimpleDateFormat(datePattern);
                            } catch (IllegalArgumentException e) {
                                datePattern = CoreConstants.ISO8601_PATTERN;
                            }
                        }

                        LvLayoutNode res = LvLayoutLog4jISO8601Date.fromPattern(datePattern);  // Optimization, LvLayoutLog4jISO8601Date works much faster.
                        return res != null ? res : new LvLayoutSimpleDateNode(datePattern);
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

                    case "p":
                    case "le":
                    case "level":
                        return new LvLayoutFixedTextNode("level", FieldTypes.LEVEL_LOGBACK,
                                Stream.of(Level.values()).map(Level::toString).toArray(String[]::new));

                    case "nopex":
                    case "nopexception":
                    case "n":
                        return null;

                    default:
                        throw new IllegalArgumentException("log encoder pattern contains unsupported terms [pattern=\""
                                + pattern + "\", term=" + n.toString() + "]");
                }

            default:
                throw new IllegalArgumentException("Unknown node type: " + n.getType() + ", pattern=" + pattern);
        }
    }

}
