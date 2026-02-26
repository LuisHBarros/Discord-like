package com.luishbarros.discord_like.modules.chat.infrastructure.websocket;
import com.luishbarros.discord_like.modules.auth.infrastructure.security.JwtHandshakeInterceptor;
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