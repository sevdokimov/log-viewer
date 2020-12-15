package com.logviewer.impl;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFileNavigationManager;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
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

    public LvFileNavigationManagerImpl(@Nonnull LvFileAccessManager fileAccessManager) {
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

    @Override
    public List<LvFsItem> getChildren(@Nullable Path path) {
        if (path != null && !path.isAbsolute())
            return null;

        if (path != null && !fileAccessManager.isDirectoryVisible(path))
            return null;

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

                return fileAccessManager.checkAccess(f) == null;
            })
                    .map(LvFsItemImpl::create)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
