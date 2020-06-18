package com.logviewer.impl;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFileNavigationManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LvFileNavigationManagerImpl implements LvFileNavigationManager {

    private final LvFileAccessManager fileAccessManager;

    private Path defaultDirectory;

    public LvFileNavigationManagerImpl(@Nonnull LvFileAccessManager fileAccessManager) {
        this.fileAccessManager = fileAccessManager;
        
        defaultDirectory = Paths.get(System.getProperty("user.home"));
    }

    @Nullable
    @Override
    public Path getDefaultDirectory() {
        return this.defaultDirectory;
    }

    public LvFileNavigationManagerImpl setDefaultDirectory(@Nullable Path defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
        return this;
    }

    @Override
    public List<LvFsItem> getChildren(@Nullable Path path) {
        if (path != null && !path.isAbsolute())
            return null;

        if (path != null && fileAccessManager.checkAccess(path) != null)
            return null;

        Stream<Path> paths;

        try {
            if (path == null) {
                paths = Stream.of(File.listRoots()).map(File::toPath);
            } else {
                paths = Files.list(path);
            }

            return paths.filter(f -> fileAccessManager.checkAccess(f) == null).map(LvFsItemImpl::new).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
