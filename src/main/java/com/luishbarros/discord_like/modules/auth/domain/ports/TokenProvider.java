package com.luishbarros.discord_like.modules.auth.domain.ports;

public interface TokenProvider {
    String generateAccessToken(Long userId);
    String generateRefreshToken(Long userId);
    Long validateAccessToken(String token);
    Long validateRefreshToken(String token);
}