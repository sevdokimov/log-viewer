package com.logviewer.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelegateProxy implements InvocationHandler {

    private static final Map<Class<?>, Map<Class<?>, Map<Method, Method>>> METHOD_MAP = new ConcurrentHashMap<>();

    private final Map<Method, Method> methodMap;

    private final Object delegate;

    public DelegateProxy(Map<Method, Method> methodMap, Object delegate) {
        this.methodMap = methodMap;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        try {
            Method delegateMethod;

            if (method.getDeclaringClass() == Object.class) {
                delegateMethod = method;
            } else {
                delegateMethod = methodMap.get(method);
            }

            return delegateMethod.invoke(delegate, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Method, Method> getDelegeteMethodsMap(Class<?> requiredClass, Class<?> delegateClass) {
        Map<Class<?>, Map<Method, Method>> clsCache = METHOD_MAP.computeIfAbsent(requiredClass, reqClass -> new ConcurrentHashMap<>());

        return clsCache.computeIfAbsent(delegateClass, delegateCls -> {
            Map<Method, Method> res = new HashMap<>();

            for (Method method : requiredClass.getMethods()) {
                try {
                    Method delegateMethod = delegateClass.getMethod(method.getName(), method.getParameterTypes());
                    res.put(method, delegateMethod);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            return res;
        });
    }

    public static <T, D> T create(Class<T> requiredClass, D delegate) {
        Map<Method, Method> methodsMap = getDelegeteMethodsMap(requiredClass, delegate.getClass());

        return (T)Proxy.newProxyInstance(DelegateProxy.class.getClassLoader(), new Class[]{requiredClass},
                new DelegateProxy(methodsMap, delegate));
    }

    public static Object getDelegate(Object proxy) {
        DelegateProxy invocationHandler = (DelegateProxy) Proxy.getInvocationHandler(proxy);
        return invocationHandler.delegate;
    }
}
