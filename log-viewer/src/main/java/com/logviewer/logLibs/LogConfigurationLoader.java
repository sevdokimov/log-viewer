package com.logviewer.logLibs;

import com.logviewer.config.LogViewerAutoConfig;
import com.logviewer.data2.LogFormat;
import org.springframework.lang.NonNull;

import java.nio.file.Path;
import java.util.Map;

/**
 * Loads the log configurations when LogViewer is embedded into a Java web application.
 * If the default log configuration detector works incorrectly, you can define a manual log configuration detector.
 * You can create a spring bean implementing {@link LogConfigurationLoader}. It will be used by {@link LogViewerAutoConfig}
 *
 */
public interface LogConfigurationLoader {

    /**
     * Loads log configurations when LogViewer is embedded into a Java web application.
     *
     * @return The map containing path to log files mapped to formats. The format may be {@code null}, in this case LogViewer
     *         will detect the format automatically.
     */
    @NonNull
    Map<Path, LogFormat> getLogConfigurations();

}
