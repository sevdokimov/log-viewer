package com.logviewer.data2.net;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;

public class Node implements Serializable {

    private final String sshUser;

    private final String host;

    private final Integer port;

    public Node(@NonNull String host) {
        this(host, null);
    }

    public Node(@NonNull String host, @Nullable Integer port) {
        this(null, host, port);
    }

    public Node(@Nullable String sshUser, @NonNull String host, @Nullable Integer port) {
        this.sshUser = sshUser;
        this.host = host;
        this.port = port;
    }

    public String getSshUser() {
        return sshUser;
    }

    @NonNull
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
