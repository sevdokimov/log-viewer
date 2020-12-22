package com.logviewer.data2;

import com.logviewer.data2.net.Node;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogPath implements Serializable {

    private static final Pattern HOST_REGEX = Pattern.compile("((?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*(?:[A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))(?::(\\d{1,5}))?");

    private final Node node;

    private final String file;

    public LogPath(@Nullable Node node, @NonNull String file) {
        this.node = node;
        this.file = file;
    }

    @Nullable
    public Node getNode() {
        return node;
    }

    @NonNull
    public String getFile() {
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogPath logPath = (LogPath) o;

        if (!Objects.equals(node, logPath.node)) return false;
        return file.equals(logPath.file);
    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + file.hashCode();
        return result;
    }

    public static List<LogPath> parsePathFromHttpParameter(@NonNull String path) {
        path = path.trim();

        int pathStart = path.lastIndexOf('@');
        if (pathStart < 0) {
            if (path.isEmpty())
                return Collections.emptyList();
            
            return Collections.singletonList(new LogPath(null, path));
        }

        String file = path.substring(0, pathStart);
        if (file.length() == 0)
            return Collections.emptyList();

        List<LogPath> res = new ArrayList<>();

        for (String hostAndPort : path.substring(pathStart + 1).split(",")) {
            Matcher matcher = HOST_REGEX.matcher(hostAndPort);
            if (!matcher.matches())
                continue;

            String host = matcher.group(1);
            Integer port = matcher.group(2) == null ? null : new Integer(matcher.group(2));

            res.add(new LogPath(new Node(host, port), file));
        }

        return res;
    }

    @Override
    public String toString() {
        if (node == null)
            return file;

        return node.toString() + '/' + file;
    }
}
