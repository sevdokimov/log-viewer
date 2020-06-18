package com.logviewer.impl;

import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.LogFormat;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Pair;
import com.logviewer.utils.PathPredicate;
import com.typesafe.config.*;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LvHoconFormatRecognizer implements LvFormatRecognizer {

    public static final String CONF_PROPERTY = "formats";

    private final List<Pair<Predicate<Path>, LogFormat>> formats = new ArrayList<>();

    public LvHoconFormatRecognizer(Config config) throws ConfigException {
        if (!config.hasPath(CONF_PROPERTY))
            return;

        List<? extends ConfigObject> formatList = config.getObjectList(CONF_PROPERTY);

        for (ConfigObject formatRecord : formatList) {
            Config elementConfig = formatRecord.toConfig();

            PathPredicate predicate = PathPredicate.fromHocon(elementConfig);

            ConfigValue format = formatRecord.get("format");
            if (format == null) {
                throw new ConfigException.BadValue(elementConfig.origin(), CONF_PROPERTY, "Missing 'format' property");
            }

            ConfigObject formatValue = elementConfig.getObject("format");
            String formatJson = formatValue.render(ConfigRenderOptions.concise());

            LogFormat logFormat = LvGsonUtils.GSON.fromJson(formatJson, LogFormat.class);

            formats.add(Pair.of(predicate, logFormat));
        }
    }

    @Nullable
    @Override
    public LogFormat getFormat(Path canonicalPath) {
        for (Pair<Predicate<Path>, LogFormat> pair : formats) {
            if (pair.getFirst().test(canonicalPath))
                return pair.getSecond();
        }

        return null;
    }
}
