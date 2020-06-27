package com.logviewer.config;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.LogFormat;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.impl.LvFileAccessManagerImpl;
import com.logviewer.logLibs.LoggerLibSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
    public LvFileAccessManager lvFileAccessManager() {
        return new LvFileAccessManagerImpl(getLogFormats().keySet());
    }

    @Bean
    public LvFormatRecognizer lvFormatRecognizer() {
        return new LvFormatRecognizer() {
            @Nullable
            @Override
            public LogFormat getFormat(Path canonicalPath) {
                return getLogFormats().get(canonicalPath);
            }
        };
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
