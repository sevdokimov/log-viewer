package com.logviewer.data2;

import java.io.IOException;

public class DirectoryNotVisibleException extends IOException {

    private final String path;

    public DirectoryNotVisibleException(String path, String message) {
        super(message);
        this.path = path;
    }
}
