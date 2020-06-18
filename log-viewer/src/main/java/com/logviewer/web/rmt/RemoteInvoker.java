package com.logviewer.web.rmt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logviewer.utils.LvGsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RemoteInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteInvoker.class);

    private static final Map<Class, Map<String, MethodDescriptor>> classCache = new ConcurrentHashMap<>();

    private static Map<String, MethodDescriptor> getMethods(@Nonnull Class handlerClass) {
        return classCache.computeIfAbsent(handlerClass, cls -> {
            Map<String, MethodDescriptor> methods = new HashMap<>();

            for (Method method : cls.getMethods()) {
                Remote remoteAnn = method.getAnnotation(Remote.class);

                if (remoteAnn != null) {
                    MethodDescriptor old = methods.put(method.getName(), new MethodDescriptor(method));

                    if (old != null)
                        throw new IllegalArgumentException("Duplicated method: " + method.getName());
                }
            }

            return methods;
        });
    }

    public static Object call(@Nonnull Object handler, @Nonnull MethodCall call) throws Throwable {
        if (LOG.isDebugEnabled())
            LOG.debug("Incoming message: " + call);

        String methodName = call.getMethodName();

        final MethodDescriptor descriptor = getMethods(handler.getClass()).get(methodName);

        if (descriptor == null)
            throw new IllegalArgumentException("Failed to find remote method '" + methodName + "' on class " + handler.getClass());

        JsonObject argsJson = call.getArgs() == null ? new JsonObject() : call.getArgs();
        Object[] args = paramsFromJsonObject(descriptor.method, argsJson);

        try {
            return descriptor.method.invoke(handler, args);
        } catch (InvocationTargetException e) {
            if (Stream.of(descriptor.method.getExceptionTypes()).anyMatch(expectedEx -> expectedEx.isInstance(e)))
                throw e; // Expected exception, return InvocationTargetException for expected exceptions.

            throw e.getTargetException(); // Unexpected exception.
        }
    }

    private static Object[] paramsFromJsonObject(Method method, JsonObject argsJson) {
        Parameter[] parameters = method.getParameters();

        Object[] res = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            assert parameter.isNamePresent();

            String paramName = parameter.getName();

            JsonElement value = argsJson.get(paramName);
            if (value != null) {
                res[i] = LvGsonUtils.GSON.fromJson(value, parameter.getParameterizedType());
            }
        }

        for (Map.Entry<String, JsonElement> entry : argsJson.entrySet()) {
            String paramName = entry.getKey();

            if (Stream.of(parameters).noneMatch(p -> p.getName().equals(paramName))) {
                throw new IllegalArgumentException("Unknown parameter [name=" + paramName + ", method=" + method);
            }
        }

        return res;
    }

    private static class MethodDescriptor {
        private final Method method;

        private final Type returnType;

        public MethodDescriptor(Method method) {
            this.method = method;

            returnType = method.getGenericReturnType();
        }
    }
}
