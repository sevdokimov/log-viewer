package com.logviewer.logLibs.log4j;

import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.AbstractPatternLogFormat;
import com.logviewer.formats.utils.LvLayoutClassNode;
import com.logviewer.formats.utils.LvLayoutDateNode;
import com.logviewer.formats.utils.LvLayoutFixedTextNode;
import com.logviewer.formats.utils.LvLayoutLog4jISO8601Date;
import com.logviewer.formats.utils.LvLayoutNode;
import com.logviewer.formats.utils.LvLayoutNumberNode;
import com.logviewer.formats.utils.LvLayoutRegexNode;
import com.logviewer.formats.utils.LvLayoutSimpleDateNode;
import com.logviewer.formats.utils.LvLayoutStretchNode;
import com.logviewer.formats.utils.LvLayoutTextNode;
import com.logviewer.utils.Triple;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Log4jLogFormat extends AbstractPatternLogFormat {

    // See org.apache.log4j.lf5.LogLevel, org.apache.log4j.Level
    private static final String[] LEVELS = {"OFF", "FATAL", "ERROR", "WARN", "INFO",
            "DEBUG", "SEVERE", "TRACE", "ALL", "WARNING", "CONFIG", "FINE", "FINER", "FINEST"
    };

    private static final Pattern LOCATION_PATTERN = Pattern.compile("" +
            "(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" +
            "\\." +
            "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" + // method name
            "\\([^)]*\\)"
    );

    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

    /**
     * Copy pasted from `org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat`
     */
    private static final Map<String, String> FIXED_FORMATS = Utils.newMap(
            "ABSOLUTE", "HH:mm:ss,SSS",
            "ABSOLUTE_MICROS", "HH:mm:ss,nnnnnn",
            "ABSOLUTE_NANOS", "HH:mm:ss,nnnnnnnnn",
            "ABSOLUTE_PERIOD", "HH:mm:ss.SSS",
            "COMPACT", "yyyyMMddHHmmssSSS",
            "DATE", "dd MMM yyyy HH:mm:ss,SSS",
            "DATE_PERIOD", "dd MMM yyyy HH:mm:ss.SSS",
            "DEFAULT", DEFAULT_FORMAT,
            "DEFAULT_MICROS", "yyyy-MM-dd HH:mm:ss,nnnnnn",
            "DEFAULT_NANOS", "yyyy-MM-dd HH:mm:ss,nnnnnnnnn",
            "DEFAULT_PERIOD", "yyyy-MM-dd HH:mm:ss.SSS",
            "ISO8601_BASIC", "yyyyMMdd'T'HHmmss,SSS",
            "ISO8601_BASIC_PERIOD", "yyyyMMdd'T'HHmmss.SSS",
            "ISO8601", "yyyy-MM-dd'T'HH:mm:ss,SSS",
            "ISO8601_OFFSET_DATE_TIME_HH", "yyyy-MM-dd'T'HH:mm:ss,SSSX",
            "ISO8601_OFFSET_DATE_TIME_HHMM", "yyyy-MM-dd'T'HH:mm:ss,SSSXX",
            "ISO8601_OFFSET_DATE_TIME_HHCMM", "yyyy-MM-dd'T'HH:mm:ss,SSSXXX",
            "ISO8601_PERIOD", "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "ISO8601_PERIOD_MICROS", "yyyy-MM-dd'T'HH:mm:ss.nnnnnn"
    );

    /**
     * See `org.apache.logging.log4j.core.pattern.DatePatternConverter.UnixFormatter`
     */
    private static final String UNIX_FORMAT = "UNIX";
    /**
     * See `org.apache.logging.log4j.core.pattern.DatePatternConverter.UnixMillisFormatter`
     */
    private static final String UNIX_MILLIS_FORMAT = "UNIX_MILLIS";

    private static final String[] ALL_CONVERTERS = {"x", "NDC", "X", "mdc", "MDC",
            "p", "level", "d", "date", "t", "tn", "thread", "threadName", "c", "logger", "fqcn", "C", "class",
            "marker", "markerSimpleName", "K", "map", "MAP", "ex", "throwable", "exception", "m", "msg", "message",
            "F", "file", "n"
            ,"L", "line", "l", "location", "M", "method", "N", "nano", "pid", "processId", "r", "relative", "sn", "sequenceNumber",
            "T", "tid", "threadId", "tp", "threadPriority", "endOfBatch", "u", "uuid"};
    static {
        Arrays.sort(ALL_CONVERTERS, Comparator.<String>comparingInt(s -> s.length()).reversed());
    }

    private final boolean realLog4j;

    public Log4jLogFormat(@NonNull String pattern) {
        this(null, pattern, true);
    }

    public Log4jLogFormat(@NonNull Charset charset, @NonNull String pattern) {
        this(charset, pattern, true);
    }

    public Log4jLogFormat(@Nullable Charset charset, @NonNull String pattern, boolean realLog4j) {
        super(charset, pattern);

        this.realLog4j = realLog4j;
    }

    @Override
    protected LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException {
        List<Triple<String, List<String>, Log4jPatternParser.FormattingInfo>> list = Log4jPatternParser.parse(pattern);

        if (list.size() > 0) { // Remove last line separator
            Triple<String, List<String>, Log4jPatternParser.FormattingInfo> lastItem = list.get(list.size() - 1);
            if (lastItem.getFirst().equals("n"))
                list.remove(list.size() - 1);
        }

        List<LvLayoutNode> nodes = new ArrayList<>();

        for (Triple<String, List<String>, Log4jPatternParser.FormattingInfo> node : list) {
            String converterName = fixConverterName(node.getFirst());

            addNodes(nodes, converterName, node);

            if (converterName.length() < node.getFirst().length())
                nodes.add(LvLayoutTextNode.of(node.getFirst().substring(converterName.length())));
        }

        mergeMessageFields(nodes);

        return nodes.toArray(new LvLayoutNode[0]);
    }

    private static String fixConverterName(@NonNull String converterName) {
        for (String converter : ALL_CONVERTERS) {
            if (converterName.startsWith(converter))
                return converter;
        }

        return converterName;
    }

    private void addNodes(List<LvLayoutNode> nodes, String converterName,
                          Triple<String, List<String>, Log4jPatternParser.FormattingInfo> converter) {
        LvLayoutNode node = toNode(converterName, converter);
        if (node != null) {
            nodes.add(node);
            return;
        }

        if (converterName.equals("x") || converterName.equals("NDC")) {
            nodes.add(LvLayoutTextNode.of("["));
            nodes.add(new LvLayoutStretchNode("ndc", FieldTypes.NDC, false, 0));
            nodes.add(LvLayoutTextNode.of("]"));
            return;
        }

        if (converterName.equals("X") || converterName.equals("mdc") || converterName.equals("MDC")) {
            node = new LvLayoutStretchNode("mdc", FieldTypes.MDC, true, 0);

            if (hasBrackets(converter.getSecond())) {
                nodes.add(LvLayoutTextNode.of("{"));
                nodes.add(node);
                nodes.add(LvLayoutTextNode.of("}"));
            } else {
                nodes.add(node);
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported pattern: %" + converterName);
    }

    /**
     * See org.apache.logging.log4j.core.pattern.MdcPatternConverter
     */
    private boolean hasBrackets(List<String> options) {
        if (options.size() == 0)  // %mdc
            return true;

        if (options.get(0).indexOf(',') > 0) // more then one key: %mdc{key1,key2}
            return true;

        return false; // no curly brackets if one key only: %mdc{key1}
    }

    /**
     * See org.apache.logging.log4j.core.util.datetime.FixedDateFormat#createIfSupported(java.lang.String...)
     */
    private static String datePatternFomOptions(List<String> options) {
        if (options.isEmpty())
            return DEFAULT_FORMAT;

        String f = options.get(0);

        if (f.indexOf('n') > 0)
            throw new IllegalArgumentException("Nonoseconds are not supported in log4J pattern");

        String fixedPattern = FIXED_FORMATS.get(f);
        if (fixedPattern != null)
            return fixedPattern;

        if (f.equals(UNIX_FORMAT) || f.equals(UNIX_MILLIS_FORMAT))
            throw new IllegalArgumentException(UNIX_FORMAT + " date format is supported in log4J pattern");

        return f;
    }

    private LvLayoutNode toNode(String converterName, Triple<String, List<String>, Log4jPatternParser.FormattingInfo> node) {
        List<String> options = node.getSecond();

        switch (converterName) {
            case "":
                return LvLayoutTextNode.of(options.get(0));

            case "p":
            case "level":
                return new LvLayoutFixedTextNode("level", (realLog4j ? FieldTypes.LEVEL_LOG4J : FieldTypes.LEVEL), LEVELS);

            case "d":
            case "date": {
                String pattern = datePatternFomOptions(options);

                LvLayoutDateNode res = LvLayoutLog4jISO8601Date.fromPattern(pattern);
                if (res == null) {
                    res = new LvLayoutSimpleDateNode(pattern);
                }

                if (options.size() > 1)
                    res = res.withTimeZone(TimeZone.getTimeZone(options.get(1)));

                return res;
            }

            case "t":
            case "tn":
            case "thread":
            case "threadName":
                return LvLayoutStretchNode.threadNode();

            case "c":
            case "logger":
            case "fqcn":
            case "C":
            case "class":
                return new LvLayoutClassNode();

            case "m":
            case "msg":
            case "message":

            case "ex":
            case "throwable":
            case "exception":

            case "K":
            case "map":
            case "MAP":

            case "marker":
            case "markerSimpleName":
                return LvLayoutStretchNode.messageNode();

            case "F":
            case "file":
                return new LvLayoutRegexNode("sourceFile", "sourceFile", SOURCE_FILE_PATTERN);

            case "n":
                throw new IllegalArgumentException("Unsupported pattern: '%n' can be at the end only");

            case "L":
            case "line":
                return new LvLayoutNumberNode("line", null, true);

            case "l":
            case "location":
                return new LvLayoutRegexNode("location", FieldTypes.MESSAGE, LOCATION_PATTERN);

            case "M":
            case "method":
                return new LvLayoutRegexNode("method", null, METHOD_PATTERN);

            case "N":
            case "nano":
                return new LvLayoutNumberNode("nano", null);

            case "pid":
            case "processId":
                return new LvLayoutNumberNode("pid", FieldTypes.PROCESS_ID);

            case "r":
            case "relative":
                return new LvLayoutNumberNode("relative", FieldTypes.RELATIVE_TIMESTAMP);

            case "sn":
            case "sequenceNumber":
                return new LvLayoutNumberNode("sn", null);

            case "T":
            case "tid":
            case "threadId":
                return new LvLayoutNumberNode("tid", null);

            case "tp":
            case "threadPriority":
                return new LvLayoutNumberNode("threadPriority", null);

            case "endOfBatch":
                return new LvLayoutFixedTextNode("endOfBatch", null, "true", "false");

            case "u":
            case "uuid":
                return new LvLayoutRegexNode("uuid", null, Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));

            default:
                return null;
        }
    }

    @Override
    public String getHumanReadableString() {
        return "log4j: " + getPattern();
    }
}
