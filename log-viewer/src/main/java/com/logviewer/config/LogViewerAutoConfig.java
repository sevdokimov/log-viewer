package com.logviewer.config;

import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.LogFormat;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.impl.LvPatternFormatRecognizer;
import com.logviewer.logLibs.LogConfigurationLoader;
import com.logviewer.logLibs.LoggerLibSupport;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.services.PathPattern;
import com.logviewer.utils.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class LogViewerAutoConfig {

    private Map<Path, LogFormat> logFormats;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${log-viewer.disable-default-configuration-loader:false}")
    private boolean disableDefaultConfigLoader;

    private Map<Path, LogFormat> getLogFormats() {
        Map<Path, LogFormat> logFormats = this.logFormats;
        if (logFormats == null) {
            logFormats = new LinkedHashMap<>();

            if (!disableDefaultConfigLoader)
                logFormats.putAll(loadLogConfiguration());

            for (LogConfigurationLoader loader : applicationContext.getBeansOfType(LogConfigurationLoader.class).values()) {
                logFormats.putAll(loader.getLogConfigurations());
            }
            
            this.logFormats = logFormats;
        }
        return logFormats;
    }

    public static Map<Path, LogFormat> loadLogConfiguration() {
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
                .filter(e -> e.getValue() != null)
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
