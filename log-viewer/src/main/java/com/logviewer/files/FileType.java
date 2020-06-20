package com.logviewer.files;

import java.util.regex.Pattern;

public class FileType {

    private final String typeId;

    private final Pattern pattern;

    private final String icon;

    public FileType(String typeId, Pattern pattern, String icon) {
        this.typeId = typeId;
        this.pattern = pattern;
        this.icon = icon;
    }

    public String getTypeId() {
        return typeId;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getIcon() {
        return icon;
    }
}
