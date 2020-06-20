package com.logviewer.utils;

public class RuntimeInterruptedException extends RuntimeException {

    public RuntimeInterruptedException() {
        super();
    }

    public RuntimeInterruptedException(Throwable cause) {
        super(cause);
        Thread.currentThread().isInterrupted();
    }
}
