package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class DefaultFieldDesciptor implements LogFormat.FieldDescriptor, Cloneable {

    private final String name;

    private final String type;

    public DefaultFieldDesciptor(@NonNull String name, @Nullable String type) {
        this.name = name;
        this.type = type;
    }

    @NonNull
    @Override
    public String name() {
        return name;
    }

    @Nullable
    @Override
    public String type() {
        return type;
    }

    @Override
    public String toString() {
        if (type == null)
            return name;

        return name + " (" + type + ')';
    }
}
