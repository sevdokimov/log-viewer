package com.logviewer.filters;

import com.logviewer.utils.Pair;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GroovyPredicateSandbox extends GroovyValueFilter {

    private static final Set<String> DISABLED_MEMBERS = new HashSet<>(Arrays.asList("invokeMethod", "setProperty",
            "getMetaClass", "setMetaClass"));

    private static final Map<Class, Pair<Boolean, Set<String>>> GREEN_CLASSES;

    static {
        Map<Class, Pair<Boolean, Set<String>>> greenClasses = new HashMap<>();

        Pair<Boolean, Set<String>> allowAll = Pair.of(true, Collections.emptySet());

        greenClasses.put(String.class, Pair.of(true, Collections.singleton("execute")));
        greenClasses.put(Pattern.class, allowAll);
        greenClasses.put(Matcher.class, allowAll);

        greenClasses.put(GroovyPredicateScriptBase.class, Pair.of(true, new HashSet<>(Arrays.asList("evaluate", "run"))));

        greenClasses.put(ArrayList.class, allowAll);
        greenClasses.put(HashSet.class, allowAll);
        greenClasses.put(HashMap.class, allowAll);
        greenClasses.put(LinkedHashMap.class, allowAll);
        greenClasses.put(LinkedHashSet.class, allowAll);

        greenClasses.put(ScriptBytecodeAdapter.class, Pair.of(false, new HashSet<>(Arrays.asList(
                "castToType", "createTuple", "createList", "createMap", "compareIdentical", "compareNotIdentical",
                "regexPattern", "findRegex", "matchRegex")))
        );

        greenClasses.put(Byte.TYPE, allowAll);
        greenClasses.put(Byte.class, allowAll);
        greenClasses.put(Short.TYPE, allowAll);
        greenClasses.put(Short.class, allowAll);
        greenClasses.put(Integer.TYPE, allowAll);
        greenClasses.put(Integer.class, allowAll);
        greenClasses.put(Float.TYPE, allowAll);
        greenClasses.put(Float.class, allowAll);
        greenClasses.put(Double.TYPE, allowAll);
        greenClasses.put(Double.class, allowAll);
        greenClasses.put(Long.TYPE, allowAll);
        greenClasses.put(Long.class, allowAll);
        greenClasses.put(Boolean.TYPE, allowAll);
        greenClasses.put(Boolean.class, allowAll);
        greenClasses.put(Character.TYPE, allowAll);
        greenClasses.put(Character.class, allowAll);
        greenClasses.put(Void.TYPE, allowAll);
        greenClasses.put(Void.class, allowAll);

        GREEN_CLASSES = greenClasses;
    }

    private final Class<? extends Script> scriptClass;

    private Map<Class, Pair<Boolean, Set<String>>> greenClasses;

    public GroovyPredicateSandbox(Class<? extends Script> scriptClass) {
        this.scriptClass = scriptClass;

        greenClasses = new LinkedHashMap<>(100, 0.4f);
        greenClasses.putAll(GREEN_CLASSES);
        greenClasses.put(scriptClass, GREEN_CLASSES.get(GroovyPredicateScriptBase.class));
    }

    public Class<? extends Script> getScriptClass() {
        return scriptClass;
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
        checkMember(receiver, method);
        
        return super.onStaticCall(invoker, receiver, method, args);
    }

    @Override
    public Object onSuperCall(Invoker invoker, Class senderType, Object receiver, String method, Object... args) throws Throwable {
        throw new SecurityException("You cannot create " + receiver.getClass() + '.' + method);
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
        if (!greenClasses.containsKey(receiver))
            throw new SecurityException("You cannot create " + receiver.getName());

        return super.onNewInstance(invoker, receiver, args);
    }

    private void checkMember(Class c, @Nullable String memberName) {
        Pair<Boolean, Set<String>> members = greenClasses.get(c);
        if (members == null)
            throw new SecurityException("You has no access to " + c);

        if (memberName == null)
            return;

        if (DISABLED_MEMBERS.contains(memberName))
            throw new SecurityException("You has no access to " + c.getSimpleName() + '.' + memberName);
        
        if (members.getFirst()) {
            // black list
            if (members.getSecond().contains(memberName))
                throw new SecurityException("You has no access to " + c.getSimpleName() + '.' + memberName);
        } else {
            // white list
            if (!members.getSecond().contains(memberName))
                throw new SecurityException("You has no access to " + c.getSimpleName() + '.' + memberName);
        }
    }

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        checkMember(receiver.getClass(), method);

        return super.onMethodCall(invoker, receiver, method, args);
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        checkMember(receiver.getClass(), property);

        return super.onGetProperty(invoker, receiver, property);
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        throw new SecurityException("You has no access to " + receiver.getClass().getSimpleName() + "." + property);
    }

    @Override
    public Object onGetAttribute(Invoker invoker, Object receiver, String attribute) throws Throwable {
        checkMember(receiver.getClass(), attribute);

        return super.onGetAttribute(invoker, receiver, attribute);
    }

    @Override
    public Object onSetAttribute(Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
        throw new SecurityException("You has no access to " + receiver.getClass().getSimpleName() + "." + attribute);
    }

    @Override
    public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        checkMember(receiver.getClass(), null);

        return super.onGetArray(invoker, receiver, index);
    }

    @Override
    public void onSuperConstructor(Invoker invoker, Class receiver, Object... args) throws Throwable {
        checkMember(receiver, null);
        super.onSuperConstructor(invoker, receiver, args);
    }

    @Override
    public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        checkMember(receiver.getClass(), null);

        return super.onSetArray(invoker, receiver, index, value);
    }
}
