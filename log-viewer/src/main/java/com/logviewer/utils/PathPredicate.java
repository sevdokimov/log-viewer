package com.logviewer.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PathPredicate implements Predicate<Path> {

    public static final String PROP_REGEXP = "regexp";
    public static final String PROP_DIRECTORY = "directory";

    private final Path directory;

    private final Pattern pattern;

    public PathPredicate(@Nullable Path directory, @Nullable Pattern pattern) {
        assert directory != null || pattern != null;

        this.directory = directory;
        this.pattern = pattern;
    }

    @Override
    public boolean test(Path path) {
        Path relative;

        if (directory != null) {
            if (!path.startsWith(directory))
                return false;

            relative = directory.relativize(path);
        } else {
            relative = path;
        }

        if (pattern != null)
            return pattern.matcher(relative.toString()).matches();

        return true;
    }

    public static PathPredicate fromHocon(Config cfg) throws ConfigException {
        Path directory = null;

        if (cfg.hasPath(PROP_DIRECTORY)) {
            String dirStr = cfg.getString(PROP_DIRECTORY);
            directory = Paths.get(dirStr);
            if (!directory.isAbsolute()) {
                throw new ConfigException.BadValue(cfg.origin(), "directory", "property must contain absolute path, " +
                        "but value is: " + dirStr);
            }
        }

        Pattern regexp = null;

        if (cfg.hasPath(PROP_REGEXP)) {
            String regexpStr = cfg.getString(PROP_REGEXP);
            try {
                regexp = Pattern.compile(regexpStr);
            } catch (Exception e) {
                throw new ConfigException.BadValue(cfg.origin(), PROP_REGEXP, "Invalid regexp: " + e.getMessage(), e );
            }
        }

        if (directory == null && regexp == null) {
            throw new ConfigException.Generic(cfg.origin().description() + " - Object must contain either 'directory' or 'regexp' property");
        }

        return new PathPredicate(directory, regexp);
    }
}
