package com.logviewer.formats;

import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.formats.utils.LvLayoutNode;
import com.logviewer.formats.utils.LvLayoutStretchNode;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

public abstract class AbstractPatternLogFormat implements LogFormat {

    /**
     * See org.apache.log4j.lf5.LogLevel, org.apache.log4j.Level, org.slf4j.event.Level, https://en.wikipedia.org/wiki/Syslog
     */
    private static final String[] LEVELS = {
            "OFF", "FATAL", "SEVERE", "EMERGENCY", "ALERT", "CRITICAL", "ERROR",
            "WARN", "WARNING",
            "INFO", "CONFIG", "NOTICE", "INFORMATIONAL",
            "DEBUG", "FINE", "FINER",
            "FINEST", "TRACE", "ALL",
    };

    protected static final String SOURCE_FILE_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.[a-z]{1,5}";

    protected static final String METHOD_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    private Charset charset;

    private Locale locale;

    private final String pattern;

    private List<String> customLevels = new ArrayList<>();

    private transient volatile DefaultFieldSet fieldSet;

    public AbstractPatternLogFormat(@NonNull String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean hasFullDate() {
        return getDelegate().hasFullDate();
    }

    public AbstractPatternLogFormat setCharset(Charset charset) {
        this.charset = charset;
        clearTemporaryState();
        return this;
    }

    public AbstractPatternLogFormat setLocale(Locale locale) {
        this.locale = locale;
        clearTemporaryState();
        return this;
    }

    protected void clearTemporaryState() {
        this.fieldSet = null;
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getCustomLevels() {
        if (customLevels == null)
            return Collections.emptyList();

        return Collections.unmodifiableList(customLevels);
    }

    protected String[] allLogLevels() {
        return Stream.concat(Stream.of(LEVELS), getCustomLevels().stream())
                .distinct()
                .toArray(String[]::new);
    }

    public AbstractPatternLogFormat addCustomLevels(@Nullable List<String> customLevels) {
        if (customLevels != null) {
            if (this.customLevels == null)
                this.customLevels = new ArrayList<>();

            this.customLevels.addAll(customLevels);
        }

        return this;
    }

    @Override
    public void loadGlobalConfig(Environment env) {
        String[] customLevels = env.getProperty("log-viewer.log-levels", String[].class);
        if (customLevels != null)
            addCustomLevels(Arrays.asList(customLevels));
    }

    @Override
    public LogReader createReader() {
        return getDelegate().createReader();
    }

    @Override
    public FieldDescriptor[] getFields() {
        return getDelegate().getFields();
    }

    protected DefaultFieldSet getDelegate() {
        DefaultFieldSet res = this.fieldSet;

        if (res == null) {
            LvLayoutNode[] nodes = parseLayout(pattern);
            res = new DefaultFieldSet(locale, charset, nodes);

            fieldSet = res;
        }

        return res;
    }

    protected abstract LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException;

    @Override
    public void validate() throws IllegalArgumentException {
        if (pattern == null || pattern.isEmpty())
            throw new RuntimeException("'pattern' field is empty");

        getDelegate();
    }

    protected static void mergeMessageFields(List<LvLayoutNode> nodes) {
        for (int i = 0; i + 1 < nodes.size(); ) {
            LvLayoutNode n1 = nodes.get(i);
            LvLayoutNode n2 = nodes.get(i + 1);

            if (n1 instanceof LvLayoutStretchNode && n2 instanceof LvLayoutStretchNode) {
                LvLayoutStretchNode ns1 = (LvLayoutStretchNode) n1;
                LvLayoutStretchNode ns2 = (LvLayoutStretchNode) n2;

                if (ns1.getFieldName().equals(ns2.getFieldName()) && Objects.equals(ns1.getFieldType(), ns2.getFieldType())) {
                    LvLayoutStretchNode merged = new LvLayoutStretchNode(ns1.getFieldName(), ns2.getFieldType(),
                            ns1.removeSpacesBefore(), ns1.getMinSize() + ns2.getMinSize());

                    nodes.set(i, merged);
                    nodes.remove(i + 1);
                    continue;
                }
            }

            i++;
        }
    }

}
