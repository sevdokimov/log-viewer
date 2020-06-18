package com.logviewer.impl;

import com.logviewer.api.LvFileAccessManager;
import com.typesafe.config.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LvFileAccessManagerImpl implements LvFileAccessManager {

    public static final String VISIBLE_DIRECTORIES_PATH = "visible-directories";

    private volatile Map<Path, Predicate<String>> allowedPaths;

    public LvFileAccessManagerImpl() {

    }

    public LvFileAccessManagerImpl(@Nonnull Collection<Path> allowedPaths) {
        setAllowedPaths(allowedPaths);
    }

    public LvFileAccessManagerImpl(@Nonnull Map<Path, Predicate<String>> allowedPaths) {
        setAllowedPaths(allowedPaths);
    }

    public LvFileAccessManagerImpl(@Nullable Config config) {
        if (config == null || !config.hasPath(VISIBLE_DIRECTORIES_PATH))
            return;

        ConfigList list = config.getList(VISIBLE_DIRECTORIES_PATH);
        if (list.isEmpty())
            return;

        allowedPaths = new HashMap<>();

        for (ConfigValue val : list) {
            if (val.valueType() == ConfigValueType.STRING) {
                allowedPaths.put(Paths.get((String)val.unwrapped()), x -> true);
            } else if (val.valueType() == ConfigValueType.OBJECT) {
                ConfigValue directoryVal = ((ConfigObject) val).get("directory");
                if (directoryVal == null) {
                    throw new IllegalArgumentException(String.format("'%s = [...]' configuration contain invalid object: 'directory' property is missing. " +
                            "'%1$s = [...]' must contains objects like '{directory: \"/path/to/log\", regexp: \".*\\\\.log\"}'", VISIBLE_DIRECTORIES_PATH));
                }
                    
                String directory = directoryVal.unwrapped().toString();
                Predicate<String> predicate = extractPredicate((ConfigObject) val);
                allowedPaths.put(Paths.get(directory), predicate);
            } else {
                throw new IllegalArgumentException(String.format("'%s = [...]' configuration property can contain list " +
                        "of strings or objects of type '{directory: \"/path/to/log\", regexp: \".*\\\\.log\"}'", VISIBLE_DIRECTORIES_PATH));
            }
        }
    }

    private Predicate<String> extractPredicate(ConfigObject val) {
        ConfigValue regexpVal = val.getOrDefault("regexp", null);
        if (regexpVal == null)
            return x -> true;

        return Pattern.compile(regexpVal.unwrapped().toString()).asPredicate();
    }

    public void setAllowedPaths(@Nonnull Map<Path, Predicate<String>> allowedPaths) {
        this.allowedPaths = new LinkedHashMap<>(allowedPaths);
    }

    public void allowAll() {
        allowedPaths = null;
    }

    public void setAllowedPaths(@Nonnull Collection<Path> allowedPaths) {
        LinkedHashMap<Path, Predicate<String>> res = new LinkedHashMap<>();
        for (Path path : allowedPaths) {
            res.put(path, null);
        }
        setAllowedPaths(res);
    }

    @Nullable
    @Override
    public String checkAccess(Path path) {
        if (allowedPaths == null)
            return null;

        for (Map.Entry<Path, Predicate<String>> entry : allowedPaths.entrySet()) {
            Path allowedPath = entry.getKey();
            if (allowedPath.startsWith(path))
                return null;

            if (path.startsWith(allowedPath)) {
                String relative = allowedPath.relativize(path).toString();
                if (entry.getValue() == null || entry.getValue().test(relative)) {
                    return null;
                }

                return "You cannot open \"" + path + "\"";
            }
        }

        return "You cannot open \"" + path + "\", the file must be located in: " + allowedPaths.keySet();
    }
}
