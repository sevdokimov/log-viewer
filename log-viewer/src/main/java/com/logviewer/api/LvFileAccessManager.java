package com.logviewer.api;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface LvFileAccessManager {
    /**
     * @param path File or directory to check,
     * @return {@code null} if user can read file, otherwise an error message.
     */
    @Nullable
    String checkAccess(Path path);

}
