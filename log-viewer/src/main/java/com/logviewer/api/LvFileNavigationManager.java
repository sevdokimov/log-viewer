package com.logviewer.api;

import com.logviewer.files.FileType;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

public interface LvFileNavigationManager {

    /**
     * @param path Directory to list or {@code null}.
     */
    @Nullable
    List<LvFsItem> getChildren(@Nullable Path path) throws SecurityException;

    @Nullable
    Path getDefaultDirectory();

    interface LvFsItem {

        Path getPath();

        boolean isDirectory();

        FileType getType();

        long getSize();

        @Nullable
        Long getModificationTime();
    }
}
