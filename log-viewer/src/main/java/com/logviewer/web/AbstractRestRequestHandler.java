package com.logviewer.web;

import com.google.gson.JsonElement;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRestRequestHandler implements AutoCloseable {

    private final ThreadLocal<LvServletRequest> request = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(AbstractRestRequestHandler.class);

    private final Map<String, Method> methods = new HashMap<>();

    protected AbstractRestRequestHandler() {
        for (Method method : getClass().getMethods()) {
            Endpoint getAnn = method.getAnnotation(Endpoint.class);
            if (getAnn == null)
                continue;

            Method oldMethod = methods.put(method.getName(), method);
            assert oldMethod == null : method.getName();
        }
    }

    @NonNull
    protected LvServletRequest getRequest() {
        return request.get();
    }

    protected Long getLongParam(String name) {
        String paramStr = getRequest().getParameter(name);

        if (paramStr == null)
            return null;

        return Long.parseLong(paramStr);
    }

    protected int getIntParam(String name) {
        return Integer.parseInt(getRequest().getParameter(name));
    }

    protected int getIntParam(String name, int defVal) {
        String s = getRequest().getParameter(name);

        if (s == null)
            return defVal;

        return Integer.parseInt(s);
    }

    private static void validateMethodType(LvServletRequest req, FormSubmitEvent.MethodType[] method) {
        try {
            FormSubmitEvent.MethodType methodType = FormSubmitEvent.MethodType.valueOf(req.getMethod());

            if (Arrays.asList(method).contains(methodType))
                return;
        } catch (IllegalArgumentException ignored) {

        }

        throw new RestException(405, "Wrong method: " + req.getMethod());
    }

    private Object loadBody(Class<?> type, LvServletRequest req) throws IOException {
        String str = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);

        if (type == String.class)
            return str;

        if (str.isEmpty())
            return null;
        
        return LvGsonUtils.GSON.fromJson(str, type);
    }

    public final void process(LvServletRequest req, LvServletResponse resp) throws IOException {
        request.set(req);

        try {
            String contextPath = req.getRequestURI();

            int idx = contextPath.lastIndexOf('/');

            String methodName = contextPath.substring(idx + 1);

            Method method = methods.get(methodName);
            if (method == null) {
                sendResponse(resp, 400, "Method not found: " + getClass().getSimpleName() + '.' + methodName + "()");
                return;
            }

            validateMethodType(req, method.getAnnotation(Endpoint.class).method());

            Object[] args;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                args = Utils.EMPTY_OBJECTS;
            } else if (Arrays.equals(parameterTypes, new Class[]{LvServletRequest.class, LvServletResponse.class})) {
                args = new Object[]{req, resp};
            } else if (Arrays.equals(parameterTypes, new Class[]{LvServletRequest.class})) {
                args = new Object[]{req};
            } else if (parameterTypes.length == 1) {
                args = new Object[]{loadBody(parameterTypes[0], req)};
            } else if (parameterTypes.length == 3 && parameterTypes[0] == LvServletRequest.class && parameterTypes[1] == LvServletResponse.class) {
                args = new Object[]{req, resp, loadBody(parameterTypes[2], req)};
            } else {
                throw new RuntimeException("Invalid method signature");
            }

            try {
                Object res;

                try {
                    res = method.invoke(this, args);
                } catch (InvocationTargetException e) {
                    Throwable targetException = e.getTargetException();

                    if (targetException instanceof RestException) {
                        sendResponse(resp, ((RestException) targetException).getCode(), targetException.getMessage());
                        return;
                    }

                    throw targetException;
                } catch (IllegalAccessException e) {
                    throw Utils.propagate(e);
                }

                if (res instanceof LvAsyncContext || method.getReturnType() == void.class)
                    return;

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");

                if (res instanceof JsonElement) {
                    LvGsonUtils.GSON.toJson((JsonElement)res, resp.getWriter());
                } else if (method.getReturnType().equals(Object.class)) {
                    LvGsonUtils.GSON.toJson(res, resp.getWriter());
                } else {
                    LvGsonUtils.GSON.toJson(res, method.getGenericReturnType(), resp.getWriter());
                }
            } catch (Throwable t) {
                log.error("Failed to process request " + req, t);

                sendResponse(resp, 500, "Internal error: " + t);
            }
        } finally {
            request.remove();
        }
    }

    private void sendResponse(LvServletResponse resp, int code, String message) throws IOException {
        if (resp.isCommitted()) {
            throw new IllegalStateException("Response already committed");
        } else {
            resp.reset();
        }

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(code);

        resp.getWriter().append(message);
    }

    @Override
    public void close() {
        // No-op.
    }
}
