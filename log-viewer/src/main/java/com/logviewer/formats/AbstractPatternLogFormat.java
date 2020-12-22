package com.logviewer.formats;

import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.formats.utils.LvLayoutNode;
import com.logviewer.formats.utils.LvLayoutStretchNode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

public abstract class AbstractPatternLogFormat implements LogFormat {

    protected static final String SOURCE_FILE_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.[a-z]{1,5}";

    protected static final String METHOD_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    private Charset charset;

    private String pattern;

    private transient volatile DefaultFieldSet fieldSet;

    public AbstractPatternLogFormat(@Nullable Charset charset, @NonNull String pattern) {
        this.charset = charset;
        this.pattern = pattern;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public boolean hasFullDate() {
        return getDelegate().hasFullDate();
    }

    public AbstractPatternLogFormat setCharset(Charset charset) {
        this.charset = charset;
        this.fieldSet = null;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public AbstractPatternLogFormat setPattern(String pattern) {
        this.pattern = pattern;
        this.fieldSet = null;
        return this;
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
            res = new DefaultFieldSet(charset, nodes);

            fieldSet = res;
        }

        return res;
    }

    protected abstract LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException;

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
