package com.logviewer;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.LogFormat;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.RegexUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class LogManager implements LvFormatRecognizer, LvFileAccessManager {

    public static final String LOGS = "logs";
    public static final String FORMAT = "format";

    private final List<LogDescriptor> descriptors;

    public LogManager(List<LogDescriptor> descriptors) {
        this.descriptors = descriptors;
    }

    @Nullable
    @Override
    public LogFormat getFormat(Path canonicalPath) {
        for (LogDescriptor descriptor : descriptors) {
            if (descriptor.getFilePattern().matcher(canonicalPath.toString()).matches()) {
                if (descriptor.getFormat() != null)
                    return descriptor.getFormat();
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String checkAccess(Path path) {
        String str = path.toString();

        for (LogDescriptor descriptor : descriptors) {
            if (descriptor.getFilePattern().matcher(str).matches())
                return null;

            if (Files.isDirectory(path)) {
                for (Pattern subdir : descriptor.getSubdirPatterns()) {
                    if (subdir.matcher(str).matches())
                        return null;
                }
            }
        }

        return "You cannot open \"" + path + "\"";
    }

    @Nonnull
    public static List<LogDescriptor> fromHocon(Config config) {
        if (!config.hasPath(LOGS))
            return Collections.emptyList();

        List<LogDescriptor> res = new ArrayList<>();

        for (ConfigObject object : config.getObjectList(LOGS)) {
            Config cfg = object.toConfig();

            String path = cfg.getString("path").replace('\\', '/');

            if (path.endsWith("/"))
                path += "**";

            List<Pattern> subdirs = new ArrayList<>();

            for (int idx = path.indexOf('/'); idx >= 0; idx = path.indexOf('/', idx + 1)) {
                if (idx == 0) {
                    subdirs.add(Pattern.compile(("/")));
                } else {
                    subdirs.add(RegexUtils.filePattern(path.substring(0, idx)));
                }
            }

            Pattern filePattern = RegexUtils.filePattern(path);

            LogFormat logFormat = null;

            if (cfg.hasPath(FORMAT)) {
                String formatJson = cfg.getObject(FORMAT).render(ConfigRenderOptions.concise());

                logFormat = LvGsonUtils.GSON.fromJson(formatJson, LogFormat.class);
            }

            res.add(new LogDescriptor(filePattern, subdirs, logFormat));
        }

        return res;
    }

}
