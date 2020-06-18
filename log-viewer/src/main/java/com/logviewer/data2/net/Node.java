package com.logviewer.data2.net;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

public class Node implements Serializable {

    private final String host;

    private final Integer port;

    public Node(@Nonnull String host) {
        this(host, null);
    }

    public Node(@Nonnull String host, @Nullable Integer port) {
        this.host = host;
        this.port = port;
    }

    @Nonnull
    public String getHost() {
        return host;
    }

    @Nullable
    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (!host.equals(node.host)) return false;
        return port != null ? port.equals(node.port) : node.port == null;
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (port == null)
            return host;
        
        return host + ':' + port;
    }
}
