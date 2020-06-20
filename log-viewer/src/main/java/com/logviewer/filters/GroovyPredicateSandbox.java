package com.logviewer.filters;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.logviewer.utils.Pair;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GroovyPredicateSandbox extends GroovyValueFilter {

    private static final Set<String> DISABLED_MEMBERS = new HashSet<>(Arrays.asList("invokeMethod", "setProperty",
            "getMetaClass", "setMetaClass"));

    private static final Map<Class, Pair<Boolean, Set<String>>> GREEN_CLASSES;

    static {
        ImmutableMap.Builder<Class, Pair<Boolean, Set<String>>> builder = ImmutableMap.<Class, Pair<Boolean, Set<String>>>builder()
                .put(String.class, Pair.of(true, Collections.singleton("execute")))
                .put(Pattern.class, Pair.of(true, Collections.emptySet()))
                .put(Matcher.class, Pair.of(true, Collections.emptySet()))

                .put(GroovyPredicateScriptBase.class, Pair.of(true, new HashSet<>(Arrays.asList("evaluate", "run"))))

                .put(ArrayList.class, Pair.of(true, Collections.emptySet()))
                .put(HashSet.class, Pair.of(true, Collections.emptySet()))
                .put(HashMap.class, Pair.of(true, Collections.emptySet()))
                .put(LinkedHashMap.class, Pair.of(true, Collections.emptySet()))
                .put(LinkedHashSet.class, Pair.of(true, Collections.emptySet()))

                .put(ScriptBytecodeAdapter.class, Pair.of(false, new HashSet<>(Arrays.asList(
                        "castToType", "createTuple", "createList", "createMap", "compareIdentical", "compareNotIdentical",
                        "regexPattern", "findRegex", "matchRegex")))
                );

        for (Class<?> primitive : Primitives.allPrimitiveTypes()) {
            builder.put(primitive, Pair.of(true, Collections.emptySet()));
            builder.put(Primitives.wrap(primitive), Pair.of(true, Collections.emptySet()));
        }

        GREEN_CLASSES = builder.build();
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
