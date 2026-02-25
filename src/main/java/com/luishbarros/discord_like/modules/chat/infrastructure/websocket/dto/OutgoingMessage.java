package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;

import java.time.Instant;

public record OutgoingMessage(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        Instant createdAt
) {}