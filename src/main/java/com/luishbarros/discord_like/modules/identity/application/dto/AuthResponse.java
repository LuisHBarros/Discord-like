package com.luishbarros.discord_like.modules.identity.application.dto;


public record AuthResponse(
        String accessToken,
        String refreshToken
) {}