package com.logviewer.impl;

import com.google.gson.JsonParseException;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.LogFormat;
import com.logviewer.services.PathPattern;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Pair;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LvPatternFormatRecognizer implements LvFormatRecognizer {

    public static final String LOGS = "logs";
    public static final String FORMAT = "format";

    private final List<Pair<PathPattern, LogFormat>> formats;

    public LvPatternFormatRecognizer(List<Pair<PathPattern, LogFormat>> formats) {
        this.formats = formats;
    }

    @Nullable
    @Override
    public LogFormat getFormat(Path canonicalPath) {
        canonicalPath = canonicalPath.toAbsolutePath();

        for (Pair<PathPattern, LogFormat> pair : formats) {
            if (pair.getFirst().matchFile(canonicalPath)) {
                if (pair.getSecond() != null) {
                    return pair.getSecond();
                }
            }

        }

        return null;
    }

    public static List<Pair<PathPattern, LogFormat>> fromHocon(Config config) {
        List<Pair<PathPattern, LogFormat>> res = new ArrayList<>();

        if (config.hasPath(LOGS)) {
            for (ConfigObject object : config.getObjectList(LOGS)) {
                Config cfg = object.toConfig();

                String path = cfg.getString("path");

                LogFormat logFormat = null;

                if (cfg.hasPath(FORMAT)) {
                    ConfigObject filterObj = cfg.getObject(FORMAT);
                    String formatJson = filterObj.render(ConfigRenderOptions.concise());

                    try {
                        logFormat = LvGsonUtils.GSON.fromJson(formatJson, LogFormat.class);
                    } catch (JsonParseException e) {
                        throw new IllegalArgumentException("Invalid configuration [line=" + filterObj.origin().lineNumber() +
                                "]: failed to load the log format", e);
                    }

                    try {
                        logFormat.validate();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid configuration [line=" + filterObj.origin().lineNumber() +
                                "]: invalid log format: " + e.getMessage(), e);
                    }
                }

                res.add(Pair.of(PathPattern.fromPattern(path), logFormat));
            }
        }

        return res;
    }

}
