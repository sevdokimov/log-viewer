package com.logviewer.impl;

import com.logviewer.api.LvFileNavigationManager;
import com.logviewer.files.FileType;
import com.logviewer.files.FileTypes;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class LvFsItemImpl implements LvFileNavigationManager.LvFsItem {
    private final Path path;

    private final BasicFileAttributes attributes;

    public LvFsItemImpl(Path path, BasicFileAttributes attributes) {
        this.path =  path;
        this.attributes = attributes;
    }

    @Override
    public boolean isDirectory() {
        return attributes.isDirectory();
    }

    @Override
    public FileType getType() {
        if (attributes.isDirectory())
            return null;

        return FileTypes.detectType(path.toString());
    }

    @Override
    public long getSize() {
        if (attributes.isDirectory())
            return -1;

        return attributes.size();
    }

    @Nullable
    @Override
    public Long getModificationTime() {
        if (attributes.isDirectory())
            return null;

        return attributes.lastModifiedTime().toMillis();
    }

    @Override
    public Path getPath() {
        return path;
    }

    public static LvFsItemImpl create(Path path) {
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException | SecurityException e) {
            return null;
        }

        return new LvFsItemImpl(path, attr);
    }
}
