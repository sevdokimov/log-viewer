package com.logviewer.logLibs;

import com.logviewer.data2.LogFormat;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LoggerLibSupport {

    private static final LoggerLibSupport[] LIB_SUPPORTS = {
            // Logback
            new LoggerLibSupport("logback",
                    "ch.qos.logback.classic.LoggerContext",
                    "com.logviewer.logLibs.logback.LogbackConfigImporter",
                    "com.logviewer.logLibs.logback.LogbackLogFormat"),

            new LoggerLibSupport("log4j",
                    "org.apache.logging.log4j.core.LoggerContext",
                    "com.logviewer.logLibs.log4j.Log4jConfigImporter",
                    "com.logviewer.logLibs.log4j.Log4jLogFormat")
    };

    private final String name;

    private final String libClassToTest;

    private final String configImporterClassName;
    private Supplier<Map<Path, LogFormat>> configImporter;

    private final String logFormatClassName;
    private Class<LogFormat> logFormatClass;

    public LoggerLibSupport(String name, String libClassToTest, String configImporterClassName, String logFormatClassName) {
        this.name = name;
        this.libClassToTest = libClassToTest;
        this.configImporterClassName = configImporterClassName;
        this.logFormatClassName = logFormatClassName;
    }

    public String getName() {
        return name;
    }

    public Stream<Class<? extends LogFormat>> getFormatClasses() {
        if (logFormatClass == null) {
            try {
                logFormatClass = (Class<LogFormat>) getClass().getClassLoader().loadClass(logFormatClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return Stream.of(logFormatClass);
    }

    public Supplier<Map<Path, LogFormat>> getConfigImporter() {
        if (configImporter == null) {
            try {
                Class<Supplier<Map<Path, LogFormat>>> cls = (Class<Supplier<Map<Path, LogFormat>>>) getClass().getClassLoader()
                        .loadClass(configImporterClassName);

                configImporter = cls.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return configImporter;

    }

    public static Stream<LoggerLibSupport> getSupportedLogLibs() {
        return Stream.of(LIB_SUPPORTS).filter(logLibs -> {
            try {
                LoggerLibSupport.class.getClassLoader().loadClass(logLibs.libClassToTest);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        });
    }

}
