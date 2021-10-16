package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogRecord;
import org.mozilla.javascript.*;
import org.springframework.lang.Nullable;

public class JsPredicate implements RecordPredicate {

    private static final String KEY = "js-context";

    private final String script;

    private transient Function compiledScript;
    private transient RuntimeException compilationError;
    private volatile transient boolean initialized;

    public JsPredicate(String script) {
        this.script = script;
    }

    public Function getCompiledScript(JsContext jsContext) {
        if (!initialized) {
            try {
                compiledScript = jsContext.cx.compileFunction(jsContext.scriptableObject, script, "JsFilter", 1, null);
            } catch (RuntimeException exception) {
                if (exception instanceof IllegalArgumentException
                        && exception.getMessage().startsWith("compileFunction only accepts source with single JS function")) {
                    exception = new IllegalArgumentException("Script must be a function. Example: \"function isVisibleEvent(text, fields) { ... }\"");
                }

                compilationError = exception;
            }

            initialized = true;
        }

        if (compilationError != null)
            throw compilationError;

        return compiledScript;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        JsContext jsContext = ctx.getProperty(KEY, name -> {
            Context cx = Context.enter();
            return new JsContext(cx);
        });

        jsContext.jsRecordObject.init(record, ctx);

        Object res = getCompiledScript(jsContext).call(jsContext.cx, jsContext.scriptableObject, jsContext.scriptableObject,
                new Object[]{record.getMessage(), jsContext.jsRecordObject});

        return jsResultToBool(res);
    }

    private boolean jsResultToBool(@Nullable Object res) {
        if (res == null || res == Undefined.instance)
            return false;

        if (res instanceof Boolean)
            return (boolean) res;

        if (res instanceof CharSequence)
            return ((CharSequence) res).length() > 0;

        if (res instanceof Number)
            return ((Number) res).doubleValue() != 0;

        return true;
    }

    private static class JsContext implements AutoCloseable {
        private final Context cx;

        private final ScriptableObject scriptableObject;

        private final JsRecordObject jsRecordObject;

        public JsContext(Context cx) {
            this.cx = cx;

            scriptableObject = cx.initSafeStandardObjects();

            jsRecordObject = new JsRecordObject(scriptableObject);
        }

        @Override
        public void close() {
            Context.exit();
        }
    }

    private static class JsRecordObject implements Scriptable {
        protected Scriptable prototype;

        protected Scriptable parent;

        private LogRecord record;
        private LogFilterContext ctx;

        private JsRecordObject(Scriptable parent) {
            this.parent = parent;
        }

        public void init(LogRecord record, LogFilterContext ctx) {
            this.record = record;
            this.ctx = ctx;
        }

        @Override
        public String getClassName() {
            return "LogRecord";
        }

        @Override
        public boolean has(int index, Scriptable start) {
            return index >= 0 && index < ctx.getFields().length;
        }

        @Override
        public boolean has(String name, Scriptable start) {
            for (LogFormat.FieldDescriptor field : ctx.getFields()) {
                if (field.name().equals(name))
                    return true;
            }

            return false;
        }

        @Override
        public Object get(String name, Scriptable start) {
            String res = record.getFieldText(name);
            return res == null ? NOT_FOUND : res;
        }

        @Override
        public Object get(int index, Scriptable start) {
            if (index < 0 || index >= ctx.getFields().length)
                return NOT_FOUND;

            LogFormat.FieldDescriptor field = ctx.getFields()[index];

            return record.getFieldText(field.name());
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            throw new RuntimeException("The object is immutable");
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            throw new RuntimeException("The object is immutable");
        }

        @Override
        public void delete(String name) {

        }

        @Override
        public void delete(int index) {

        }

        @Override
        public boolean hasInstance(Scriptable instance) {
            return false;
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
            throw new RuntimeException();
        }

        @Override
        public void setParentScope(Scriptable parent) {
            this.parent = parent;
        }

        @Override
        public Scriptable getParentScope() {
            return parent;
        }

        @Override
        public Object[] getIds() {
            LogFormat.FieldDescriptor[] fields = ctx.getFields();

            Object[] res = new Object[fields.length];

            for (int i = 0; i < fields.length; i++) {
                res[i] = fields[i].name();
            }

            return res;
        }

        @Override
        public Scriptable getPrototype() {
            return prototype;
        }

        @Override
        public void setPrototype(Scriptable prototype) {
            this.prototype = prototype;
        }
    }
}
