package com.logviewer.config;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFileNavigationManager;
import com.logviewer.api.LvFilterStorage;
import com.logviewer.api.LvPermalinkStorage;
import com.logviewer.data2.*;
import com.logviewer.data2.config.ConfigDirHolder;
import com.logviewer.data2.config.ConfigDirHolderImpl;
import com.logviewer.data2.net.RemoteNodeService;
import com.logviewer.impl.LvFileAccessManagerImpl;
import com.logviewer.impl.LvFileNavigationManagerImpl;
import com.logviewer.services.FileSystemFilterStorage;
import com.logviewer.services.LvPermalinkStorageImpl;
import com.logviewer.utils.LvTimer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LvConfigBase {

    @Bean
    public RemoteNodeService lvRemoteNodeService() {
        return new RemoteNodeService();
    }

    @Bean
    public RemoteLogChangeListenerService lvRemoteLogChangeListenerService(RemoteNodeService remoteNodeService) {
        return new RemoteLogChangeListenerService(remoteNodeService);
    }

    @Bean
    public FileWatcherService lvFileWatcherService() {
        return new FileWatcherService();
    }

    @Bean
    public FavoriteLogService lvFavoriteLogService(ConfigDirHolder configDir) {
        return new FileFavoriteLogService(configDir);
    }

    @Bean
    public LvPermalinkStorage lvPermalinkService(ConfigDirHolder configDir) {
        return new LvPermalinkStorageImpl(configDir);
    }

    @Bean
    public LvFilterStorage lvFilterStorage(ConfigDirHolder configDir) {
        return new FileSystemFilterStorage(configDir);
    }

    @Bean
    public LogService lvLogService() {
        return new LogService();
    }

    @Bean
    public LvFileAccessManager lvFileAccessManager() {
        return new LvFileAccessManagerImpl();
    }

    @Bean
    public LvFileNavigationManager lvFileNavigationManager(LvFileAccessManager fileAccessManager) {
        return new LvFileNavigationManagerImpl(fileAccessManager);
    }

    @Bean
    public ConfigDirHolder lvConfigDirHolder(Environment environment) {
        return new ConfigDirHolderImpl(environment);
    }

    @Bean(destroyMethod = "cancel")
    public LvTimer lvTimer() {
        return new LvTimer();
    }
}
