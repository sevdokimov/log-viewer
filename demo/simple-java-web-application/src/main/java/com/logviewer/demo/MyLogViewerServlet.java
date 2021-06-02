package com.logviewer.demo;

import com.logviewer.config.LogViewerAutoConfig;
import com.logviewer.config.LvConfigBase;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.web.LogViewerServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyLogViewerServlet extends LogViewerServlet {

    @Override
    public void init() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(LogViewerAutoConfig.class, LvConfigBase.class);
        LogContextHolder.setInstance(ctx);

        super.init();
    }

    @Override
    protected ApplicationContext getSpringContext() {
        return super.getSpringContext();
    }
}
