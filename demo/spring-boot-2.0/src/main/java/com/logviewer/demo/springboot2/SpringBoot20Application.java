package com.logviewer.demo.springboot2;

import com.logviewer.demo.LogGeneratorService;
import com.logviewer.springboot.LogViewerSpringBootConfig;
import com.logviewer.springboot.LogViewerWebsocketConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({LogViewerSpringBootConfig.class, LogViewerWebsocketConfig.class})
public class SpringBoot20Application {

	@Bean
	public LogGeneratorService logGenerator() {
		return new LogGeneratorService();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBoot20Application.class, args);
	}

}
