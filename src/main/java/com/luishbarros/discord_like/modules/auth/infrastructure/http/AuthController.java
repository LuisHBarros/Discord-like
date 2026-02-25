package com.luishbarros.discord_like.modules.auth.infrastructure.http;

import com.luishbarros.discord_like.modules.auth.application.dto.AuthResponse;
import com.luishbarros.discord_like.modules.auth.application.dto.LoginRequest;
import com.luishbarros.discord_like.modules.auth.application.dto.RefreshRequest;
import com.luishbarros.discord_like.modules.auth.application.dto.RegisterRequest;
import com.luishbarros.discord_like.modules.auth.application.service.AuthService;
import com.luishbarros.discord_like.shared.adapters.ratelimit.RateLimitedAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RateLimitedAuthService authService;

    public AuthController(RateLimitedAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody  RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String accessToken,
                                       @Valid @RequestBody RefreshRequest request) {
        authService.logout(extractToken(accessToken), request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}