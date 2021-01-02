package com.logviewer.api;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.List;

public interface LvFileAccessManager {
    /**
     * @param path File or directory to check,
     * @return {@code null} if user can read file, otherwise an error message.
     */
    @Nullable
    boolean isFileVisible(Path path);

    boolean isDirectoryVisible(Path dir);

    String errorMessage(Path path);

    @NonNull
    List<Path> getRoots();
}
