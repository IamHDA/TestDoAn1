package com.vn.backend.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix cho các message từ server gửi đến client
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các message từ client gửi đến server
        config.setApplicationDestinationPrefixes("/app");

        // Prefix cho message gửi đến user cụ thể
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") //TODO: chỉ định domain FE cụ thể
                .withSockJS();
    }
}
