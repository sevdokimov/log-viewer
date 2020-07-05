package com.logviewer;

import com.logviewer.api.LvPathResolver;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.net.Node;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class HoconPathResolver implements LvPathResolver {

    private final Map<String, List<LogPath>> paths;

    public HoconPathResolver(@Nonnull ConfigObject configObj) {
        Map<String, List<LogPath>> paths = new HashMap<>();

        Config cfg = configObj.toConfig();

        for (String path : configObj.keySet()) {
            Config onePathCfg = cfg.getConfig(path);

            List<String> files = toListString(onePathCfg, "file");

            List<Node> nodes;

            if (onePathCfg.hasPath("host")) {
                List<String> hosts = toListString(onePathCfg, "host");

                Integer port = onePathCfg.hasPath("port") ? onePathCfg.getInt("port") : null;

                nodes = hosts.stream().map(host -> new Node(host, port)).collect(Collectors.toList());
            } else {
                nodes = Collections.singletonList(null);
            }

            List<LogPath> pathList = files.stream()
                    .flatMap(file -> nodes.stream().map(n -> new LogPath(n, file)))
                    .collect(Collectors.toList());

            paths.put(path, pathList);
        }

        this.paths = paths;
    }

    private static List<String> toListString(Config cfg, String path) {
        ConfigValue value = cfg.getValue(path);

        Object stringOrStringList = value.unwrapped();
        if (stringOrStringList instanceof String)
            return Collections.singletonList((String)stringOrStringList);

        if (stringOrStringList instanceof List) {
            List l = (List) stringOrStringList;

            for (Object o : l) {
                if (!(o instanceof String))
                    throw new ConfigException.WrongType(value.origin(), "Wrong List element, List of string expected");
            }

            return l;
        }

        throw new ConfigException.WrongType(value.origin(), "String or List of String expected");
    }

    @Nullable
    @Override
    public Collection<LogPath> resolvePath(@Nonnull String pathFromHttpParameter) {
        return paths.get(pathFromHttpParameter);
    }
}
