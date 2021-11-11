LogViewer can be added to existing Spring Boot application as a library. The library provides the log-viewer servlet.
You can map the servlet to any URL and it will show logs. Log configuration will be detected automatically.

The library is `io.github.sevdokimov.logviewer:log-viewer-spring-boot:1.0.1`

<details>
  <summary>Maven configuration</summary>
  <p>
  
```xml
<dependencies>
    <dependency>
        <groupId>io.github.sevdokimov.logviewer</groupId>
        <artifactId>log-viewer-spring-boot</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```
  </p>
</details>

<details>
  <summary>Gradle configuration</summary>
  <p>
  
```groovy
dependencies {
    implementation 'io.github.sevdokimov.logviewer:log-viewer-spring-boot:1.0.1'
}
```
  </p>
</details>

When the log-viewer library is added, add `com.logviewer.springboot.LogViewerSpringBootConfig` configuration class to the Spring configuration.<br>
That's all! LogViewerSpringBootConfig maps `LogViewerServlet` servlet to "/logs" URL, list of log files and its format will
be detected automatically.

URL for `LogViewerServlet` servlet mapping can be specified by `log-viewer.url-mapping` property.

You can specify the list of available logs manually, if LogViewer cannot detect it automatically. Create a spring bean of type
`com.logviewer.logLibs.LogConfigurationLoader` that has `Map<Path, LogFormat> getLogConfigurations()` method.
This method returns a map containing paths to log files mapped to formats. The format may be "null", in this case 
LogViewer will detect the format automatically.  The default log configuration detection can be disabled by 
`log-viewer.disable-default-configuration-loader=true` property.

If your application support WebSocket, add `com.logviewer.springboot.LogViewerWebsocketConfig` configuration class too.
It switches UI&nbsp;&#x27f7;&nbsp;Backend interaction to WebSocket protocol. WebSocket works slightly faster than HTTP requests.<br>
Path for WebSocket endpoint can be specified by `log-viewer.websocket.path` property.