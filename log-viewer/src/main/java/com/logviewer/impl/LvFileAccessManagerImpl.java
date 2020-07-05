package com.logviewer.impl;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.utils.RegexUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LvFileAccessManagerImpl implements LvFileAccessManager {

    public static final String VISIBLE_DIRECTORIES_PATH = "visible-directories";

    private volatile Map<Path, Pattern> allowedPaths;

    public LvFileAccessManagerImpl() {

    }

    public LvFileAccessManagerImpl(@Nonnull Collection<Path> allowedPaths) {
        setAllowedPaths(allowedPaths);
    }

    public LvFileAccessManagerImpl(@Nullable Config config) {
        if (config == null || !config.hasPath(VISIBLE_DIRECTORIES_PATH))
            return;

        allowedPaths = new HashMap<>();

        for (ConfigValue val : config.getList(VISIBLE_DIRECTORIES_PATH)) {
            if (val.valueType() == ConfigValueType.STRING) {
                allowedPaths.put(Paths.get((String)val.unwrapped()), null);
            } else if (val.valueType() == ConfigValueType.OBJECT) {
                ConfigValue directoryVal = ((ConfigObject) val).get("directory");
                if (directoryVal == null) {
                    throw new IllegalArgumentException(String.format("'%s = [...]' configuration contain invalid object: 'directory' property is missing. " +
                            "'%1$s = [...]' must contains objects like '{directory: \"/path/to/log\", regex: \".*\\\\.log\"}'", VISIBLE_DIRECTORIES_PATH));
                }
                    
                String directory = directoryVal.unwrapped().toString();
                Pattern predicate = extractPredicate((ConfigObject) val);
                allowedPaths.put(Paths.get(directory), predicate);
            } else {
                throw new IllegalArgumentException(String.format("'%s = [...]' configuration property can contain list " +
                        "of strings or objects of type '{directory: \"/path/to/log\", regex: \".*\\\\.log\"}'", VISIBLE_DIRECTORIES_PATH));
            }
        }
    }

    private Pattern extractPredicate(ConfigObject val) {
        ConfigValue regexVal = val.get("regex");
        if (regexVal != null)
            return Pattern.compile(regexVal.unwrapped().toString());

        ConfigValue fileVal = val.get("file");
        if (fileVal != null) {
            return RegexUtils.filePattern(fileVal.unwrapped().toString());
        }

        return null;
    }

    public void setAllowedPaths(@Nonnull Map<Path, Pattern> allowedPaths) {
        this.allowedPaths = new LinkedHashMap<>(allowedPaths);
    }

    public void allowAll() {
        allowedPaths = null;
    }

    public void setAllowedPaths(@Nonnull Collection<Path> allowedPaths) {
        LinkedHashMap<Path, Pattern> res = new LinkedHashMap<>();
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

        for (Map.Entry<Path, Pattern> entry : allowedPaths.entrySet()) {
            Path allowedPath = entry.getKey();
            if (allowedPath.startsWith(path))
                return null;

            if (path.startsWith(allowedPath)) {
                String relative = allowedPath.relativize(path).toString();
                if (entry.getValue() == null || entry.getValue().matcher(relative).matches()) {
                    return null;
                }

                return "You cannot open \"" + path + "\"";
            }
        }

        return "You cannot open \"" + path + "\", the file must be located in: " + allowedPaths.keySet();
    }
}
