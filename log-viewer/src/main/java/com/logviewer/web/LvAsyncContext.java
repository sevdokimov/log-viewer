package com.logviewer.web;

public interface LvAsyncContext {

    void complete();

    void setTimeout(long timeout);

    Object getResponse();

}
