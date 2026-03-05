package com.luishbarros.discord_like.modules.collaboration.application.dto;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long roomId,
        Long senderId,
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
                message.getContent().ciphertext(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.isEdited()
        );
    }
}
