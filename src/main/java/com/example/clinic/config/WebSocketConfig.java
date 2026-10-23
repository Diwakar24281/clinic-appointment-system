package com.example.clinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP configuration for real-time emergency alerts.
 * - /ws : WebSocket endpoint for STOMP connection
 * - /topic/* : Broadcast topics (e.g., /topic/emergencies for hospitals, /topic/doctor/{id} for doctors)
 * - /queue/* : User-specific messages (e.g., /queue/emergency-updates for patient tracking)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for /topic and /queue destinations
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoints with SockJS fallback
        registry.addEndpoint("/ws", "/ws-emergency")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
