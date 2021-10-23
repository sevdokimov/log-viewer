package com.logviewer;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvPathResolver;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.net.Node;
import com.logviewer.data2.net.RemoteNodeService;
import com.logviewer.data2.net.server.api.RemoteContext;
import com.logviewer.utils.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HoconPathResolver implements LvPathResolver {

    private static final Logger LOG = LoggerFactory.getLogger(HoconPathResolver.class);

    private final Map<String, Triple<List<Node>, List<String>, List<Pair<Path, Pattern>>>> paths;

    @Autowired
    private RemoteNodeService remoteNodeService;
    @Autowired
    private LvFileAccessManager fileAccessManager;

    public HoconPathResolver(@NonNull ConfigObject configObj) {
        Map<String, Triple<List<Node>, List<String>, List<Pair<Path, Pattern>>>> paths = new HashMap<>();

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

            List<String> fixedPaths = new ArrayList<>();
            List<Pair<Path, Pattern>> patternPaths = new ArrayList<>();

            for (String file : files) {
                if (file.contains("*")) {
                    patternPaths.add(parsePattern(onePathCfg, file));
                } else {
                    fixedPaths.add(file);
                }
            }

            paths.put(path, Triple.create(nodes, fixedPaths, patternPaths));
        }

        this.paths = paths;
    }

    private static Pair<Path, Pattern> parsePattern(Config onePathCfg, @NonNull String pattern) throws IllegalArgumentException {
        String normalizedPattern = Utils.normalizePath(pattern);

        int firstWildCardIdx = normalizedPattern.indexOf("*");
        assert firstWildCardIdx >= 0;

        int lastSlashIdx = normalizedPattern.lastIndexOf('/');

        if (firstWildCardIdx < lastSlashIdx)
            throw new IllegalArgumentException("Invalid configuration at " + onePathCfg.origin().description()
                    + " : invalid file pattern, a file pattern must start with absolute path. Wrong pattern: " + pattern);

        Path dir = Paths.get(normalizedPattern.substring(0, lastSlashIdx));

        if (!dir.isAbsolute()) {
            throw new IllegalArgumentException("Invalid configuration at " + onePathCfg.origin().description()
                    + "invalid file pattern: '*' can be in the file name only, not in the middle of the path." +
                    " '/opt/my-app/logs/*.log' is correct, but '/opt/*/logs/foo.log' is not correct. Wrong pattern: " + pattern);
        }

        Pattern fileNameRegexp = RegexUtils.filePattern(normalizedPattern.substring(lastSlashIdx + 1));

        return Pair.of(dir, fileNameRegexp);
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

    private static List<Path> resolvePattern(LvFileAccessManager fileAccessManager, @NonNull Path dir, @NonNull Pattern filePattern) {
        if (!fileAccessManager.isDirectoryVisible(dir)) {
            LOG.warn("Failed to load log list from the directory, LogViewer configuration prohibits access to the directory {}", dir);
            return Collections.emptyList();
        }

        try {
            return Files.list(dir)
                    .filter(f -> {
                        if (!filePattern.matcher(f.getFileName().toString()).matches())
                            return false;

                        return fileAccessManager.isFileVisible(f);
                    })
                    .collect(Collectors.toList());
        } catch (NoSuchFileException ignored) {

        } catch (IOException e) {
            LOG.error("Failed to scan directory {}", dir, e);
        }

        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<LogPath> resolvePath(@NonNull String pathFromHttpParameter) {
        Triple<List<Node>, List<String>, List<Pair<Path, Pattern>>> triple = paths.get(pathFromHttpParameter);
        if (triple == null)
            return null;

        Set<LogPath> res = new LinkedHashSet<>();

        Map<Node, Future<List<String>>> remotePaths = new HashMap<>();

        for (Node node : triple.getFirst()) {
            for (String fixedPath : triple.getSecond()) {
                res.add(new LogPath(node, fixedPath));
            }

            if (triple.getThird().size() > 0) {
                if (node == null) {
                    for (Pair<Path, Pattern> pair : triple.getThird()) {
                        for (Path path : resolvePattern(fileAccessManager, pair.getFirst(), pair.getSecond())) {
                            res.add(new LogPath(node, path.toString()));
                        }
                    }

                    continue;
                }

                CompletableFuture<List<String>> future = remoteNodeService.getNodeConnection(node)
                        .thenCompose(out -> out.execute(new RemoteLogListResolver(triple.getThird())));

                remotePaths.put(node, future);
            }
        }

        for (Map.Entry<Node, Future<List<String>>> entry : remotePaths.entrySet()) {
            Future<List<String>> future = entry.getValue();
            Node node = entry.getKey();

            try {
                List<String> logPaths = future.get();

                for (String logPath : logPaths) {
                    res.add(new LogPath(node, logPath));
                }
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            } catch (ExecutionException e) {
                LOG.warn("Failed to load log list from remote node: {}", node, e);
            }
        }

        return res.stream().sorted(Comparator.comparing(LogPath::getFile)).collect(Collectors.toList());
    }

    public static class RemoteLogListResolver implements Function<RemoteContext, List<String>>, Serializable {

        private static final long serialVersionUID = 0L;

        private final List<Pair<String, Pattern>> paths;

        public RemoteLogListResolver(List<Pair<Path, Pattern>> paths) {
            this.paths = paths.stream()
                    .map(p -> Pair.of(p.getFirst().toString(), p.getSecond()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<String> apply(RemoteContext remoteContext) {
            LvFileAccessManager accessManager = remoteContext.getLogService().getAccessManager();

            List<String> res = new ArrayList<>();

            for (Pair<String, Pattern> pair : paths) {
                for (Path path : resolvePattern(accessManager, Paths.get(pair.getFirst()), pair.getSecond())) {
                    res.add(path.toString());
                }
            }

            return res;
        }
    }
}
