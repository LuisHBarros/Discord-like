// auth/infrastructure/security/JwtTokenProvider.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidTokenError;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessKey, accessExpirationMs);
    }

    @Override
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshKey, refreshExpirationMs);
    }

    @Override
    public Long validateAccessToken(String token) {
        return extractUserId(token, accessKey);
    }

    @Override
    public Long validateRefreshToken(String token) {
        return extractUserId(token, refreshKey);
    }

    private String generateToken(Long userId, SecretKey key, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private Long extractUserId(String token, SecretKey key) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenError("Invalid or expired token");
        }
    }

    @Override
    public Instant getInspiration(String token){
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().toInstant();
        } catch (JwtException e){
            return  Instant.now();
        }
    }
}