package com.logviewer.filters;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class GroovyPredicate implements RecordPredicate {

    private final String script;

    private static final Cache<String, Object> scriptCache = CacheBuilder.newBuilder().maximumSize(500).build();

    private transient GroovyPredicateSandbox sandbox;
    private transient CompilationFailedException compilationError;

    private volatile transient boolean initialized;

    /**
     * @param script
     */
    public GroovyPredicate(@Nonnull String script) {
        this.script = script;
    }

    public GroovyPredicateSandbox getSandbox() {
        if (!initialized) {
            try {
                Object o = scriptCache.get(script, () -> {
                    CompilerConfiguration cfg = new CompilerConfiguration();
                    cfg.setScriptBaseClass(GroovyPredicateScriptBase.class.getName());
                    cfg.addCompilationCustomizers(new SandboxTransformer());

                    GroovyClassLoader cl = new GroovyClassLoader(getClass().getClassLoader(), cfg);

                    try {
                        Class scriptClass = cl.parseClass(this.script, "GroovyFilterClass");
                        return new GroovyPredicateSandbox(scriptClass);
                    } catch (CompilationFailedException e) {
                        return e;
                    }
                });

                if (o instanceof GroovyPredicateSandbox) {
                    sandbox = (GroovyPredicateSandbox) o;
                }
                else {
                    compilationError = (CompilationFailedException) o;
                }

                initialized = true;
            } catch (ExecutionException e) {
                throw Throwables.propagate(e.getCause());
            }
        }

        return sandbox;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        GroovyPredicateSandbox sandbox = getSandbox();

        if (sandbox == null)
            throw compilationError;

        sandbox.register();

        try {
            Script script = sandbox.getScriptClass().newInstance();

            script.setBinding(new RecordBinding(record, ctx));

            Object res = script.run();

            return Boolean.TRUE.equals(res);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to create script instance");
        } finally {
            sandbox.unregister();
        }
    }

    private static final class RecordBinding extends Binding {
        private final Record record;
        private final LogFilterContext ctx;

        RecordBinding(Record record, LogFilterContext ctx) {
            this.record = record;
            this.ctx = ctx;
        }

        @Override
        public Object getVariable(String name) {
            if (Record.WHOLE_LINE.equals(name))
                return record.getMessage();

            return ctx.getFieldValue(record, name);
        }

        @Override
        public void setVariable(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasVariable(String name) {
            if (Record.WHOLE_LINE.equals(name))
                return true;

            return ctx.findFieldIndexByName(name) >= 0;
        }

        @Override
        public Map getVariables() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getProperty(String property) {
            return getVariable(property);
        }

        @Override
        public void setProperty(String property, Object newValue) {
            throw new UnsupportedOperationException();
        }
    }
}
