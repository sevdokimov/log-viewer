package com.logviewer.demo.springboot2;

import com.logviewer.demo.LogGeneratorService;
import com.logviewer.springboot.LogViewerSpringBootConfig;
import com.logviewer.springboot.LogViewerWebsocketConfig;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({LogViewerSpringBootConfig.class, LogViewerWebsocketConfig.class})
public class SpringBoot23Application {

	@Bean
	public LogGeneratorService logGenerator() {
		return new LogGeneratorService();
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SpringBoot23Application.class, args);

		LoggerFactory.getLogger(SpringBoot23Application.class).info("\n\n\n{}\n\n", context.getBean(LogGeneratorService.class).logViewerUrl());
	}

}
