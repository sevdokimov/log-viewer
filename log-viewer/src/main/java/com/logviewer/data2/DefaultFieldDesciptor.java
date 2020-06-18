package com.logviewer.data2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultFieldDesciptor implements LogFormat.FieldDescriptor, Cloneable {

    private final String name;

    private final String type;

    public DefaultFieldDesciptor(@Nonnull String name, @Nullable String type) {
        this.name = name;
        this.type = type;
    }

    @Nonnull
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
