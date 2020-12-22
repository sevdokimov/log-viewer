package com.logviewer.services;

import com.logviewer.api.LvFileAccessManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LvFileAccessManagerImpl implements LvFileAccessManager {

    static Supplier<List<Path>> ROOT_PROVIDER = () -> Stream.of(File.listRoots()).map(File::toPath).collect(Collectors.toList());

    private List<PathPattern> descriptors;

    private List<Path> roots;

    /**
     * @param descriptors List of visible logs. {@code null} means allow all logs.
     */
    public LvFileAccessManagerImpl(@Nullable List<PathPattern> descriptors) {
        setPaths(descriptors);
    }

    private LvFileAccessManagerImpl() {
        this(null);
    }

    /**
     * @param descriptors List of visible logs. {@code null} means allow all logs.
     */
    public void setPaths(@Nullable List<PathPattern> descriptors) {
        if (descriptors == null) {
            this.descriptors = null;
            this.roots = ROOT_PROVIDER.get();
        } else {
            this.descriptors = new ArrayList<>(descriptors);
            roots = computeRoots(descriptors);
        }
    }

    public void setVisibleFiles(@NonNull List<Path> files) {
        setPaths(files.stream().map(PathPattern::file).collect(Collectors.toList()));
    }

    public void allowAll() {
        setPaths(null);
    }

    private static List<Path> computeRoots(List<PathPattern> descriptors) {
        List<Path> fixedPaths = new ArrayList<>();

        for (PathPattern descriptor : descriptors) {
            if (descriptor.getPrefix() == null)
                return ROOT_PROVIDER.get();

            fixedPaths.add(descriptor.getPrefix());
        }

        Set<Path> copy = new HashSet<>(fixedPaths);

        for (Path path : copy) {
            fixedPaths.removeIf(p -> path != p && p.startsWith(path));
        }

        return fixedPaths;
    }

    @Nullable
    @Override
    public String checkAccess(Path file) {
        if (!file.isAbsolute())
            throw new IllegalArgumentException("Path is not absolute: " + file);

        if (descriptors == null)
            return null;

        for (PathPattern descriptor : descriptors) {
            if (descriptor.matchFile(file))
                return null;
        }

        return "You cannot open \"" + file + "\"";
    }

    @Override
    public boolean isDirectoryVisible(Path dir) {
        if (!dir.isAbsolute())
            throw new IllegalArgumentException("Path is not absolute: " + dir);

        if (descriptors == null)
            return true;

        for (PathPattern descriptor : descriptors) {
            if (descriptor.matchDir(dir))
                return true;
        }

        return false;
    }

    @NonNull
    @Override
    public List<Path> getRoots() {
        return roots;
    }
}
