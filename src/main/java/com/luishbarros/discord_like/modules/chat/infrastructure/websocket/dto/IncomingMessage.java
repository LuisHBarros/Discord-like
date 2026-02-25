package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;


public record IncomingMessage(
        String type,
        Long roomId,
        String content,
        Long senderId
) {}