package com.logviewer.tests.utils;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebTestUtils {

    public static Map<String, String> queryParameters(String str) {
        try {
            return queryParameters(new URL(str));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> queryParameters(URL url) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return query_pairs;
    }

    public static Path getDownloadDirectory() {
        try {
            URL sampleResourceUrl = Resources.getResource("integration/data/search.log");
            assert sampleResourceUrl.getProtocol().equals("file");

            Path samplePath = Paths.get(sampleResourceUrl.getFile());

            Path classesDir = samplePath.getParent().getParent().getParent();

            Path tempDirectory = classesDir.resolve("download-dir");

            if (!Files.isDirectory(tempDirectory))
                Files.createDirectory(tempDirectory);

            return tempDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
