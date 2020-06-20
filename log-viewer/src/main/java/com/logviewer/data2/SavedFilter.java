package com.logviewer.data2;

public class SavedFilter {

    private final String name;
    private final String value;

    public SavedFilter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
