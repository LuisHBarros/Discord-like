package com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto;

import java.util.UUID;

public record IncomingMessage(
        String type,
        UUID roomId,
        String content,
        UUID senderId
) {}