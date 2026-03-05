package com.luishbarros.discord_like.shared.adapters.config;

import com.luishbarros.discord_like.modules.identity.infrastructure.security.JwtHandshakeInterceptor;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final JwtHandshakeInterceptor jwtInterceptor;

    public WebSocketConfig(ChatWebSocketHandler handler, JwtHandshakeInterceptor jwtInterceptor) {
        this.handler = handler;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/rooms/*")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
    }
}
