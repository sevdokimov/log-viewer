package com.logviewer.services;

import com.logviewer.utils.RegexUtils;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PathPattern {

    private final Path prefix;
    private final Predicate<Path> fileFilter;
    private final Predicate<Path> dirFilter;

    public PathPattern(@Nullable Path prefix, @NonNull Predicate<Path> fileFilter, @NonNull Predicate<Path> dirFilter) {
        this.prefix = prefix;
        this.fileFilter = fileFilter;
        this.dirFilter = dirFilter;
    }

    @Nullable
    public Path getPrefix() {
        return prefix;
    }

    @NonNull
    public Predicate<Path> getFileFilter() {
        return fileFilter;
    }

    @NonNull
    public Predicate<Path> getDirFilter() {
        return dirFilter;
    }

    public boolean matchFile(@NonNull Path file) {
        if (prefix == null)
            return fileFilter.test(file);

        if (!file.startsWith(prefix))
            return false;

        if (prefix.getNameCount() == file.getNameCount())
            return false;

        Path relative = prefix.relativize(file);

        return fileFilter.test(relative);
    }

    public boolean matchDir(@NonNull Path dir) {
        if (prefix == null)
            return dirFilter.test(dir);

        if (prefix.startsWith(dir))
            return true;

        if (!dir.startsWith(prefix))
            return false;

        Path relative = dir.subpath(prefix.getNameCount(), dir.getNameCount());
        return dirFilter.test(relative);
    }

    public static PathPattern directory(@NonNull Path dir) {
        if (!dir.isAbsolute())
            throw new IllegalArgumentException("Path must be absolute: " + dir);

        return new PathPattern(dir, p -> true, p -> true);
    }

    public static PathPattern file(@NonNull Path file) {
        if (!file.isAbsolute())
            throw new IllegalArgumentException("Path must be absolute: " + file);

        if (file.getParent() == null)
            throw new IllegalArgumentException("Path is not a file: " + file);

        Path fileName = file.getFileName();

        return new PathPattern(file.getParent(), f -> f.equals(fileName), dir -> false);
    }

    public static PathPattern fromPattern(@NonNull String pattern) {
        pattern = Utils.normalizePath(pattern);
        if (pattern.endsWith("/"))
            pattern += "**";

        String fixedPrefix = extractFixedPrefix(pattern);
        Path fixedPrefixPath;

        if (fixedPrefix == null) {
            fixedPrefixPath = null;
        } else {
            Path path = Paths.get(fixedPrefix);
            fixedPrefixPath = path.isAbsolute() ? path : null;
        }

        if (fixedPrefixPath == null) {
            Pattern p = RegexUtils.filePattern("**/" + pattern);
            Predicate<Path> filePredicate = path -> p.matcher(path.toString()).matches();

            return new PathPattern(null, filePredicate, path -> true);
        }

        int tailStart;

        if (fixedPrefix.equals("/")) {
            tailStart = 1;
        } else {
            assert pattern.startsWith(fixedPrefix + '/');
            tailStart = fixedPrefix.length() + 1;
        }

        List<Pattern> subdirs = new ArrayList<>();

        for (int idx = pattern.indexOf('/', tailStart); idx >= 0; idx = pattern.indexOf('/', idx + 1)) {
            Pattern regex = RegexUtils.filePattern(pattern.substring(tailStart, idx));
            subdirs.add(regex);
        }

        if (pattern.endsWith("/**"))
            subdirs.add(RegexUtils.filePattern(pattern.substring(tailStart)));

        Predicate<Path> dirPredicate = path -> {
            String str = path.toString();

            for (Pattern subdir : subdirs) {
                if (subdir.matcher(str).matches())
                    return true;
            }

            return false;
        };

        Pattern filePattern = RegexUtils.filePattern(pattern.substring(tailStart));

        Predicate<Path> filePredicate = path -> filePattern.matcher(path.toString()).matches();

        return new PathPattern(fixedPrefixPath, filePredicate, dirPredicate);
    }

    @Nullable
    private static String extractFixedPrefix(@NonNull String pattern) {
        int firstStarIdx = pattern.indexOf('*');
        if (firstStarIdx < 0) {
            int slashIdx = pattern.lastIndexOf('/');
            if (slashIdx < 0)
                return null;

            return pattern.substring(0, slashIdx);
        }

        int slashIdx = pattern.lastIndexOf('/', firstStarIdx - 1);
        if (slashIdx < 0)
            return null;

        if (slashIdx == 0)
            return "/";

        return pattern.substring(0, slashIdx);
    }
}
