package com.logviewer.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

public interface LvFileAccessManager {
    /**
     * @param path File or directory to check,
     * @return {@code null} if user can read file, otherwise an error message.
     */
    @Nullable
    String checkAccess(Path path);

    boolean isDirectoryVisible(Path dir);

    @Nonnull
    List<Path> getRoots();
}
