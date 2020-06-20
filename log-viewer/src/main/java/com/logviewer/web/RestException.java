package com.logviewer.web;

/**
 *
 */
public class RestException extends RuntimeException {

    private final int code;

    public RestException(int code) {
        this(code, null);
    }

    public RestException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
