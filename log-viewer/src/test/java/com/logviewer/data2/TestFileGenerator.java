package com.logviewer.data2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestFileGenerator {

    private static Path root;

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            throw new IllegalArgumentException();

        root = Paths.get(args[0]);

        create("buffered-file/empty.log", "");
        create("buffered-file/n3.log", "\n\n\n");
        create("buffered-file/rn3.log", "\r\n\r\n\r\n");
        create("buffered-file/single-line.log", "abc");
        create("buffered-file/a__bc_edf_.log", "a\r\nbc\nedf\n");
    }

    private static void create(String name, String data) throws IOException {
        Path file = root.resolve(name);

        if (!Files.exists(file)) {
            Files.write(file, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        }
    }

}
