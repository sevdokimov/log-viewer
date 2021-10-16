package com.logviewer.web.rmt;

import com.google.gson.JsonObject;

public class MethodCall {

    private String methodName;

    private JsonObject args;

    public String getMethodName() {
        return methodName;
    }

    public JsonObject getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return methodName + '(' + args + ')';
    }
}
