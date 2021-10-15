package com.logviewer.web;

import com.google.gson.JsonElement;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRestRequestHandler implements AutoCloseable {

    private final ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(AbstractRestRequestHandler.class);

    private final Map<String, Method> methods = new HashMap<>();

    protected AbstractRestRequestHandler() {
        for (Method method : getClass().getMethods()) {
            Endpoint getAnn = method.getAnnotation(Endpoint.class);
            if (getAnn == null)
                continue;

            if (getAnn.method() == FormSubmitEvent.MethodType.GET) {
                assert method.getParameterCount() == 0 : method.getName();
            }
            else {
                assert method.getParameterCount() <= 1 : method.getName();
            }

            Method oldMethod = methods.put(method.getName(), method);
            assert oldMethod == null : method.getName();
        }
    }

    public final void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FormSubmitEvent.MethodType methodType;

        if (req.getMethod().equals("GET")) {
            methodType = FormSubmitEvent.MethodType.GET;
        }
        else if (req.getMethod().equals("POST")) {
            methodType = FormSubmitEvent.MethodType.POST;
        }
        else {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        process(req, resp, methodType);
    }

    @NonNull
    protected HttpServletRequest getRequest() {
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

    private void process(HttpServletRequest req, HttpServletResponse resp, FormSubmitEvent.MethodType methodType) throws IOException {
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

            if (method.getAnnotation(Endpoint.class).method() != methodType) {
                sendResponse(resp, 405, "Wrong method: " + methodType);
                return;
            }

            Object[] args;

            switch (methodType) {
                case GET:
                    args = Utils.EMPTY_OBJECTS;
                    break;

                case POST: {
                    if (method.getParameterCount() == 0) {
                        args = Utils.EMPTY_OBJECTS;
                    }
                    else if (method.getParameterCount() == 1) {
                        Object arg;

                        Class<?> paramType = method.getParameterTypes()[0];
                        if (paramType == String.class) {
                            arg = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
                        }
                        else {
                            arg = LvGsonUtils.GSON.fromJson(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8), paramType);
                        }

                        args = new Object[]{arg};
                    }
                    else {
                        throw new RuntimeException();
                    }
                    break;
                }

                default:
                    throw new RuntimeException();
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

                if (res instanceof AsyncContext)
                    return;

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");

                if (method.getReturnType() == void.class) {
                    resp.getWriter().print("true");
                } else if (res instanceof JsonElement) {
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

    private void sendResponse(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(code);

        if (resp.isCommitted()) {
            throw new IllegalStateException("Response already committed");
        } else {
            resp.resetBuffer();
        }

        resp.getWriter().append(message);
    }

    @Override
    public void close() {
        // No-op.
    }
}
