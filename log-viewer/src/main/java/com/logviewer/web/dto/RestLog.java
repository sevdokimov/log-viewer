package com.logviewer.web.dto;

import com.logviewer.data2.LogView;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestLog {

    private final String id;
    private final String path;
    private final String node;
    private final String url;
    private final boolean connected;

    private final List<RestField> fields;

    public RestLog(LogView log) {
        id = log.getId();
        node = log.getHostname();
        path = log.getPath().getFile();
        connected = log.isConnected();
        url = log.getPath().getNode() == null ? log.getPath().getFile() : log.getPath().getNode().toString() + ',' + log.getPath().getFile();
        fields = Stream.of(log.getFormat().getFields()).map(RestField::new).collect(Collectors.toList());
    }

}
