package com.logviewer.utils;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteCallUtils {

    private static final ConcurrentMap<Class, ClassDescription> classDescriptions = new ConcurrentHashMap<>();

    public static ClassDescription getClassDescription(Class cls) {
        return classDescriptions.computeIfAbsent(cls, c -> {
            ClassDescription res = new ClassDescription(c);

            for (Method method : c.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) && Modifier.isAbstract(method.getModifiers())) {
                    Hasher hasher = Hashing.md5().newHasher();
                    hasher.putUnencodedChars(method.getName());
                    hasher.putInt(method.getParameterCount());
                    for (Class<?> paramType : method.getParameterTypes()) {
                        hasher.putUnencodedChars(paramType.getName());
                    }

                    Integer hash = hasher.hash().asInt();
                    res.method2id.put(method, hash);
                    res.id2Method.put(hash, method);
                }
            }

            assert res.method2id.size() == res.id2Method.size();
            return res;
        });
    }

    public static class ClassDescription {
        private final Class cls;
        private final Map<Method, Integer> method2id = new HashMap<>();
        private final Map<Integer, Method> id2Method = new HashMap<>();

        private ClassDescription(Class cls) {
            this.cls = cls;
        }

        public Integer getIdByMethod(Method method) {
            return method2id.get(method);
        }

        public Method getMethodById(Integer id) {
            return id2Method.get(id);
        }

        public Object call(Object target, Object[] args, Integer methodId) throws Throwable {
            Method method = getMethodById(methodId);
            if (method == null)
                throw new IllegalArgumentException("Unknown method [interface=" + cls + ", methodId=" + methodId + ']');

            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
