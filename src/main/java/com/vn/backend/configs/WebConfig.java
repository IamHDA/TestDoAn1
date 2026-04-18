package com.vn.backend.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public DeviceInfoInterceptor deviceInfoInterceptor() {
        return new DeviceInfoInterceptor();
    }
    
    @Bean
    public TraceIdInterceptor traceIdInterceptor() {
        return new TraceIdInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor())
                .addPathPatterns("/**");
        registry.addInterceptor(deviceInfoInterceptor())
                .addPathPatterns("/**");
    }
}

