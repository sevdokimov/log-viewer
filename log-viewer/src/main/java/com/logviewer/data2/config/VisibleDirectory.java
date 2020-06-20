package com.logviewer.data2.config;

import com.google.common.base.Strings;
import com.logviewer.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class VisibleDirectory {

    private final String directory;

    private final String fileRegexp;

    private transient volatile Pattern pattern;

    public VisibleDirectory(@Nonnull String directory) {
        this(directory, null);
    }

    public VisibleDirectory(@Nonnull String directory, @Nullable String fileRegexp) {
        this.directory = directory;
        this.fileRegexp = Strings.emptyToNull(fileRegexp);
    }

    public String getDirectory() {
        return directory;
    }

    public String getFileRegexp() {
        return fileRegexp;
    }

    public boolean match(@Nonnull String path) {
        if (!Utils.isSubdirectory(directory, path))
            return false;

        if (fileRegexp == null)
            return true;

        String relativePath = path.substring(directory.length());

        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);

        if (relativePath.isEmpty())
            return true;

        Pattern pattern = this.pattern;
        if (pattern == null) {
            pattern = Pattern.compile(fileRegexp);
            this.pattern = pattern;
        }

        return pattern.matcher(relativePath).matches();
    }

    @Override
    public String toString() {
        if (fileRegexp == null)
            return directory;

        return directory + " [~" + fileRegexp + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VisibleDirectory that = (VisibleDirectory) o;

        return directory.equals(that.directory)
                && (fileRegexp != null ? fileRegexp.equals(that.fileRegexp) : that.fileRegexp == null);
    }

    @Override
    public int hashCode() {
        int result = directory.hashCode();
        result = 31 * result + (fileRegexp != null ? fileRegexp.hashCode() : 0);
        return result;
    }
}
