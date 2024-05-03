package com.logviewer.web;

import com.logviewer.data2.LogContextHolder;
import com.logviewer.utils.DelegateProxy;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LogViewerServlet extends HttpServlet {

    private final LogViewerServletHandler handler = new LogViewerServletHandler();

    @Override
    public void init() {
        handler.init(getSpringContext(), getServletConfig().getInitParameter(LogViewerServletHandler.WEB_SOCKET_PATH));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handler.doPost(DelegateProxy.create(LvServletRequest.class, req), DelegateProxy.create(LvServletResponse.class, resp));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handler.doGet(DelegateProxy.create(LvServletRequest.class, req), DelegateProxy.create(LvServletResponse.class, resp));
    }

    @Override
    public void destroy() {
        handler.destroy();
    }

    protected ApplicationContext getSpringContext() {
        ApplicationContext appCtx = LogContextHolder.getInstance();
        if (appCtx != null)
            return appCtx;

        appCtx = (ApplicationContext) getServletConfig().getServletContext().getAttribute(LogViewerServletHandler.SPRING_CONTEXT_PROPERTY);
        if (appCtx != null)
            return appCtx;

        throw new IllegalStateException("Spring context not found. Set ApplicationContext to " +
                "com.logviewer.data2.LogContextHolder.setInstance(appCtx)");
    }
}
