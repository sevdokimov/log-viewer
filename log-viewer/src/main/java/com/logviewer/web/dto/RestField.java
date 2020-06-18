package com.logviewer.web.dto;

import com.logviewer.data2.LogFormat;

public class RestField {
    private String name;
    private String type;

    public RestField(LogFormat.FieldDescriptor descriptor) {
        this.name = descriptor.name();
        this.type = descriptor.type();
    }
}
