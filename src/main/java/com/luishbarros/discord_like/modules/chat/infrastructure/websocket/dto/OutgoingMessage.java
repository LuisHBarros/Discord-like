package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record OutgoingMessage(
        UUID messageId,
        UUID roomId,
        UUID senderId,
        String content,
        Instant createdAt
) {}