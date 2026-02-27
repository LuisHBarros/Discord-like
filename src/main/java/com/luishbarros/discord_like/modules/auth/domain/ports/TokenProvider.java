package com.luishbarros.discord_like.modules.auth.domain.ports;

import java.time.Instant;

public interface TokenProvider {
    String generateAccessToken(Long userId);
    String generateRefreshToken(Long userId);
    Long validateAccessToken(String token);
    Long validateRefreshToken(String token);
    Instant getInspiration(String token);
}