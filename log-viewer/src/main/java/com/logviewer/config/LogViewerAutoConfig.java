package com.logviewer.config;

import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.LogFormat;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.impl.LvPatternFormatRecognizer;
import com.logviewer.logLibs.LoggerLibSupport;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.services.PathPattern;
import com.logviewer.utils.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class LogViewerAutoConfig {

    private Map<Path, LogFormat> logFormats;

    private Map<Path, LogFormat> getLogFormats() {
        Map<Path, LogFormat> logFormats = this.logFormats;
        if (logFormats == null) {
            logFormats = loadConfiguredLogs();
            this.logFormats = logFormats;
        }
        return logFormats;
    }

    protected Map<Path, LogFormat> loadConfiguredLogs() {
        Map<Path, LogFormat> res = new HashMap<>();

        LoggerLibSupport.getSupportedLogLibs().forEach(libSupport -> {
            res.putAll(libSupport.getConfigImporter().get());
        });

        return res;
    }

    @Bean
    public LvFileAccessManagerImpl lvLogManager() {
        LvFileAccessManagerImpl res = new LvFileAccessManagerImpl(null);
        res.setVisibleFiles(new ArrayList<>(getLogFormats().keySet()));
        return res;
    }

    @Bean
    public LvFormatRecognizer logFormatRecognizer() {
        List<Pair<PathPattern, LogFormat>> pairs = getLogFormats().entrySet().stream()
                .map(e -> Pair.of(PathPattern.file(e.getKey()), e.getValue()))
                .collect(Collectors.toList());

        return new LvPatternFormatRecognizer(pairs);
    }

    @Bean
    public FavoriteLogService lvFavoriteLogService() {
        InmemoryFavoritesService res = new InmemoryFavoritesService();

        for (Path path : getLogFormats().keySet()) {
            res.addFavoriteLog(path.toString());
        }

        res.setEditable(false);

        return res;
    }

}
