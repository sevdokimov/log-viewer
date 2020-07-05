package com.logviewer;

import com.typesafe.config.Config;
import org.springframework.core.env.PropertySource;

public class TypesafeConfigPropertySource extends PropertySource<Config> {
    public TypesafeConfigPropertySource(String name, Config source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String path) {
        if (path.indexOf(':') >= 0)
            return null; // Property name with default value, for example: @Value("${log-viewer.server.port:8111}")
            
        if (source.hasPath(path)) {
            return source.getAnyRef(path);
        }
        
        return null;
    }
}
