package com.luishbarros.discord_like.modules.chat.application.dto;

import com.luishbarros.discord_like.modules.chat.domain.model.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID roomId,
        UUID senderId,
        String content,
        Instant createdAt,
        Instant editedAt,
        boolean edited
) {
    public static MessageResponse fromMessage(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getCiphertext(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.isEdited()
        );
    }
}
