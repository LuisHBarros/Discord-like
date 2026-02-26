package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;

public record ConnectResponse(
        String type,
        String message
) {
    public static ConnectResponse of(String message) {
        return new ConnectResponse("info", message);
    }
}
