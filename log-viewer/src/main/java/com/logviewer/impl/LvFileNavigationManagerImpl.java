package com.logviewer.impl;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFileNavigationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LvFileNavigationManagerImpl implements LvFileNavigationManager {

    private final LvFileAccessManager fileAccessManager;

    @Value("${log-viewer.default-directory:}")
    private String defaultDirectory;

    public LvFileNavigationManagerImpl(@NonNull LvFileAccessManager fileAccessManager) {
        this.fileAccessManager = fileAccessManager;
    }

    @Nullable
    @Override
    public Path getDefaultDirectory() {
        if (!defaultDirectory.isEmpty())
            return Paths.get(defaultDirectory);

        List<Path> roots = fileAccessManager.getRoots();

        if (roots.stream().allMatch(r -> r.getParent() == null)) {
            Path userHomePath = Paths.get(System.getProperty("user.home"));

            if (fileAccessManager.isDirectoryVisible(userHomePath))
                return userHomePath;
        }

        if (roots.size() == 1)
            return roots.get(0);

        return null; // null means list of roots (c:\ , d:\ , f:\)
    }

    @NonNull
    @Override
    public List<LvFsItem> getChildren(@Nullable Path path) throws SecurityException, IOException {
        if (path != null && !path.isAbsolute())
            throw new SecurityException("path must be absolute");

        if (path != null && !fileAccessManager.isDirectoryVisible(path))
            throw new SecurityException(fileAccessManager.errorMessage(path));

        Stream<Path> paths;

        try {
            if (path == null) {
                paths = fileAccessManager.getRoots().stream();
            } else {
                paths = Files.list(path);
            }

            return paths.filter(f -> {
                if (Files.isDirectory(f)) {
                    return fileAccessManager.isDirectoryVisible(f);
                }

                return fileAccessManager.isFileVisible(f);
            })
                    .map(LvFsItemImpl::create)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException e) {
            throw new SecurityException("Not enough permissions to access file or directory");
        }
    }

    // For tests only.
    public LvFileNavigationManagerImpl setDefaultDirectory(@NonNull String defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
        return this;
    }
}
