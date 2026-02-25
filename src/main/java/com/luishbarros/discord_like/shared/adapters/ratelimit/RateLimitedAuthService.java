// shared/adapters/ratelimit/RateLimitedAuthService.java
package com.luishbarros.discord_like.shared.adapters.ratelimit;

import com.luishbarros.discord_like.modules.auth.application.dto.*;
import com.luishbarros.discord_like.modules.auth.application.service.AuthService;
import com.luishbarros.discord_like.shared.domain.error.RateLimitError;
import com.luishbarros.discord_like.shared.ports.RateLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Primary
@Component
public class RateLimitedAuthService {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public RateLimitedAuthService(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    public AuthResponse register(RegisterRequest request, String clientIp) {
        checkLimit(clientIp);
        return authService.register(request);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        checkLimit(clientIp);
        return authService.login(request);
    }

    public AuthResponse refresh(String refreshToken) {
        return authService.refresh(refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        authService.logout(accessToken, refreshToken);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        authService.changePassword(userId, currentPassword, newPassword);
    }

    public void deactivate(Long userId) {
        authService.deactivate(userId);
    }

    public void activate(Long userId) {
        authService.activate(userId);
    }

    private void checkLimit(String key) {
        if (!rateLimiter.isAllowed(key, MAX_REQUESTS, WINDOW_SECONDS)) {
            throw new RateLimitError(key);
        }
    }
}