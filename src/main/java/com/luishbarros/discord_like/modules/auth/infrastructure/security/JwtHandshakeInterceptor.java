package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidTokenError;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;

    public JwtHandshakeInterceptor(TokenProvider tokenProvider, TokenBlacklist tokenBlacklist) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || tokenBlacklist.isBlacklisted(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Long userId = tokenProvider.validateAccessToken(token);
            attributes.put("userId", userId);
            return true;
        } catch (InvalidTokenError e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token != null) return token;

        List<String> auth = request.getHeaders().get("Authorization");
        if (auth != null && !auth.isEmpty() && auth.getFirst().startsWith("Bearer ")) {
            return auth.getFirst().substring(7);
        }
        return null;
    }
}