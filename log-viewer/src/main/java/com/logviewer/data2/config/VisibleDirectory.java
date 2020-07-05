package com.logviewer.data2.config;

import com.google.common.base.Strings;
import com.logviewer.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class VisibleDirectory {

    private final String directory;

    private final String fileRegex;

    private transient volatile Pattern pattern;

    public VisibleDirectory(@Nonnull String directory) {
        this(directory, null);
    }

    public VisibleDirectory(@Nonnull String directory, @Nullable String fileRegex) {
        this.directory = directory;
        this.fileRegex = Strings.emptyToNull(fileRegex);
    }

    public String getDirectory() {
        return directory;
    }

    public String getFileRegex() {
        return fileRegex;
    }

    public boolean match(@Nonnull String path) {
        if (!Utils.isSubdirectory(directory, path))
            return false;

        if (fileRegex == null)
            return true;

        String relativePath = path.substring(directory.length());

        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);

        if (relativePath.isEmpty())
            return true;

        Pattern pattern = this.pattern;
        if (pattern == null) {
            pattern = Pattern.compile(fileRegex);
            this.pattern = pattern;
        }

        return pattern.matcher(relativePath).matches();
    }

    @Override
    public String toString() {
        if (fileRegex == null)
            return directory;

        return directory + " [~" + fileRegex + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VisibleDirectory that = (VisibleDirectory) o;

        return directory.equals(that.directory)
                && (fileRegex != null ? fileRegex.equals(that.fileRegex) : that.fileRegex == null);
    }

    @Override
    public int hashCode() {
        int result = directory.hashCode();
        result = 31 * result + (fileRegex != null ? fileRegex.hashCode() : 0);
        return result;
    }
}
