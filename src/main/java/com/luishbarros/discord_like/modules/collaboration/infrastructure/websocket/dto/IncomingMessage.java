package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto;


public record IncomingMessage(
        String type,
        Long roomId,
        String content
) {}