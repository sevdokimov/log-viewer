package com.logviewer.data2;

import org.springframework.context.ApplicationContext;

public class LogContextHolder {

    private static ApplicationContext instance;

    private LogContextHolder() {

    }

    public static ApplicationContext getInstance() {
        return instance;
    }

    public static void setInstance(ApplicationContext instance) {
        LogContextHolder.instance = instance;
    }
}
