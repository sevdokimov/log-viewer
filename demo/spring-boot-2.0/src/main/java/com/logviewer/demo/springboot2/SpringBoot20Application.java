package com.logviewer.demo.springboot2;

import com.logviewer.demo.LogGeneratorService;
import com.logviewer.logLibs.LogConfigurationLoader;
import com.logviewer.springboot.LogViewerSpringBootConfig;
import com.logviewer.springboot.LogViewerWebsocketConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;
import java.util.Collections;

@SpringBootApplication
@Import({LogViewerSpringBootConfig.class, LogViewerWebsocketConfig.class})
public class SpringBoot20Application {

	@Bean
	public LogGeneratorService logGenerator() {
		return new LogGeneratorService();
	}

	/**
	 * Adds "/var/log/syslog" to the list of logs visible by LogViewer
	 */
	@Bean
	public LogConfigurationLoader systemLogConfigLoader() {
		return () -> Collections.singletonMap(Paths.get("/var/log/syslog"), null);
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBoot20Application.class, args);
	}

}
