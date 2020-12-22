package com.logviewer.data2;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public class FileAttributes implements Serializable {

    private final long size;

    private final long modifiedTime;

    public FileAttributes(long size, long modifiedTime) {
        this.size = size;
        this.modifiedTime = modifiedTime;
    }

    public FileAttributes(BasicFileAttributes attrs) {
        this(attrs.size(), attrs.lastModifiedTime().toMillis());
    }

    public long getSize() {
        return size;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileAttributes)) return false;
        FileAttributes that = (FileAttributes) o;
        return size == that.getSize() &&
                modifiedTime == that.getModifiedTime();
    }

    @Override
    public int hashCode() {
        return (int) (size * 31 + modifiedTime);
    }

    @Override
    public String toString() {
        return "{size=" + size + ", time=" + new Date(modifiedTime) + '}';
    }

    @Nullable
    public static FileAttributes fromPath(Path file) throws IOException {
        try {
            return new FileAttributes(Files.readAttributes(file, BasicFileAttributes.class));
        } catch (NoSuchFileException e) {
            return null;
        }
    }
}
