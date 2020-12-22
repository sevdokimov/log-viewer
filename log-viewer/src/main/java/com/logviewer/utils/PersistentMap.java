package com.logviewer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class PersistentMap {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentMap.class);

    private final Path path;
    private final int limit;

    private LinkedHashMap<String, String> map;

    private int memoryUsage;

    public PersistentMap(Path path, int limit) {
        this.path = path;
        this.limit = limit;
    }

    public Path getFile() {
        return path;
    }

    private void load() throws IOException {
        assert map == null;
        assert memoryUsage == 0;

        map = new LinkedHashMap<>();

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int size = in.readInt();

            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                map.put(key, value);
                memoryUsage += entrySize(key, value);
            }
        }
    }

    private static int entrySize(String key, String value) {
        return stringSize(key) + stringSize(value);
    }

    private static int stringSize(String s) {
        return s.length() * 2 + 16;
    }

    private void ensureInited() {
        if (map == null) {
            try {
                load();
            } catch (IOException e) {
                if (!(e instanceof NoSuchFileException))
                    LOG.error("Failed to load data", e);

                map = new LinkedHashMap<>();
            }
        }
    }

    public synchronized String get(@NonNull String key) {
        ensureInited();

        String value = map.remove(key);

        if (value != null)
            map.put(key, value);

        return value;
    }

    public synchronized void put(@NonNull String key, @NonNull String value) {
        ensureInited();

        String oldValue = map.remove(key);
        map.put(key, value);

        if (oldValue == null) {
            memoryUsage += entrySize(key, value);
        }
        else {
            if (oldValue.equals(value))
                return;

            memoryUsage += stringSize(value) - stringSize(oldValue);
        }

        while (memoryUsage > limit && map.size() > 1) {
            String firstKey = map.keySet().iterator().next();

            String v = map.remove(firstKey);
            memoryUsage -= entrySize(key, v);
        }

        try (DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            outputStream.writeInt(map.size());

            for (Map.Entry<String, String> entry : map.entrySet()) {
                outputStream.writeUTF(entry.getKey());
                outputStream.writeUTF(entry.getValue());
            }
        } catch (IOException e) {
            LOG.error("Failed to save state: " + path, e);
        }
    }

    public int getMemoryUsage() {
        return memoryUsage;
    }
}
