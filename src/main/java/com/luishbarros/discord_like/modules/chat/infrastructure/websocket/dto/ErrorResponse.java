package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String code,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code, LocalDateTime.now());
    }
}