package com.logviewer.utils;

import java.io.ByteArrayOutputStream;

public class OpenByteArrayOutputStream extends ByteArrayOutputStream {

    public OpenByteArrayOutputStream() {
    }

    public OpenByteArrayOutputStream(int size) {
        super(size);
    }

    public byte[] getBuffer() {
        return buf;
    }

}
