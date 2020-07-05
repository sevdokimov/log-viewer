package com.logviewer.utils;

public interface Destroyer extends AutoCloseable {

    @Override
    void close();
}
