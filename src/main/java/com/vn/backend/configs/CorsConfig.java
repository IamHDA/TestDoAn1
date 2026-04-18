package com.vn.backend.configs;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EntityScan("com.vn.backend.entities")
@EnableJpaRepositories("com.vn.backend.repositories")
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigure() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                .exposedHeaders("Authorization")
                .maxAge(3600);
            }
        };
    }
}
