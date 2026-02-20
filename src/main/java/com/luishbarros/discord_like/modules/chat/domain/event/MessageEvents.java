package com.luishbarros.discord_like.modules.chat.domain.event;
import com.luishbarros.discord_like.modules.chat.domain.model.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageEvents(
        UUID messageId,
        UUID roomId,
        UUID senderId,
        String ciphertext,
        Instant editedAt,
        EventType type
) {

    public enum EventType {
        CREATED,
        EDITED,
        DELETED
    }

    public static MessageEvents created(Message message) {
        return new MessageEvents(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getCiphertext(),
                null,
                EventType.CREATED
        );
    }

    public static MessageEvents edited(Message message) {
        return new MessageEvents(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getCiphertext(),
                message.getEditedAt(),
                EventType.EDITED
        );
    }

    public static MessageEvents deleted(Message message) {
        return new MessageEvents(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getCiphertext(),
                null,
                EventType.DELETED
        );
    }
}

