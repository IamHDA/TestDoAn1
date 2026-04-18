package com.vn.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		log.info("=== Starting Backend Application ===");
		log.info("Active profile: {}", System.getProperty("spring.profiles.active"));
		log.info("Log directory: /app/logs");
		
		SpringApplication.run(BackendApplication.class, args);
		
		log.info("=== Backend Application Started Successfully ===");
	}

}
