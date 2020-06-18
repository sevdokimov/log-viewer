package com.logviewer.filters;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;
import com.logviewer.utils.Pair;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.annotation.Nonnull;
import javax.script.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class JsPredicate implements RecordPredicate {

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    private final String script;

    private static final Cache<String, Object> scriptCache = CacheBuilder.newBuilder().maximumSize(500).build();

    private transient CompiledScript scriptInstance;
    private transient Throwable scriptError;

    private transient volatile ThreadLocal<Pair<ScriptObjectMirror, SimpleScriptContext>> local = new ThreadLocal<>();

    private volatile transient boolean initialized;

    public JsPredicate(@Nonnull String script) {
        this.script = script;
    }

    public CompiledScript getScriptInstance() {
        if (!initialized) {
            try {
                Object o = scriptCache.get(script, () -> {
                    NashornScriptEngine engine = (NashornScriptEngine)scriptEngineManager.getEngineByName("JavaScript");

                    try {
                        return engine.compile(script);
                    } catch (ScriptException e) {
                        return e;
                    }
                });

                if (o instanceof Throwable) {
                    scriptError = (Throwable) o;
                }
                else {
                    scriptInstance = (CompiledScript) o;
                }
            } catch (ExecutionException e) {
                throw Throwables.propagate(e.getCause());
            }

            initialized = true;
        }

        return scriptInstance;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        CompiledScript script = getScriptInstance();

        if (script == null)
            return false;

        try {
            RecordBinding bindings = new RecordBinding(record, ctx);

            Pair<ScriptObjectMirror, SimpleScriptContext> pair = local.get();
            if (pair == null) {
                ScriptContext ctxt = script.getEngine().getContext();

                SimpleScriptContext tempctxt = new SimpleScriptContext();
                tempctxt.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                tempctxt.setBindings(ctxt.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
                tempctxt.setWriter(ctxt.getWriter());
                tempctxt.setReader(ctxt.getReader());
                tempctxt.setErrorWriter(ctxt.getErrorWriter());

                pair = Pair.of(null, tempctxt);
                local.set(pair);
            }
            else {
                pair.getSecond().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            }

            Object res;

            if (pair.getFirst() != null) {
                pair.getFirst().clear();
                bindings.put("nashorn.global", pair.getFirst());

                res = script.eval(pair.getSecond());
            }
            else {
                try {
                    res = script.eval(pair.getSecond());
                } finally {
                    pair.setFirst((ScriptObjectMirror) bindings.get("nashorn.global"));
                }
            }

            return Boolean.TRUE.equals(res);
        }catch (Exception ex) {
            return false;
        }
    }

    private static final class RecordBinding extends AbstractMap<String, Object> implements Bindings {
        private final Record record;
        private final LogFilterContext ctx;

        private final Map<String, Object> variables = new HashMap<>();

        RecordBinding(Record record, LogFilterContext ctx) {
            this.record = record;
            this.ctx = ctx;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            if (Record.WHOLE_LINE.equals(key))
                return true;

            if (!(key instanceof String))
                return false;

            return variables.containsKey(key) || ctx.findFieldIndexByName((String) key) >= 0;
        }

        @Override
        public Object get(Object key) {
            if (!(key instanceof String))
                return false;

            if (Record.WHOLE_LINE.equals(key))
                return record.getMessage();

            Object res = variables.get(key);
            if (res != null)
                return res;

            return ctx.getFieldValue(record, (String)key);
        }

        @Override
        public Object put(String key, Object value) {
            return variables.put(key, value);
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }
}
