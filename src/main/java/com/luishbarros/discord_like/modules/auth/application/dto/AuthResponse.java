package com.luishbarros.discord_like.modules.auth.application.dto;


public record AuthResponse(
        String accessToken,
        String refreshToken
) {}