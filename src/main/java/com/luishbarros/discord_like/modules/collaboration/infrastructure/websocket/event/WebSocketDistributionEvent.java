package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.event;

import java.time.Instant;
import java.util.UUID;

public record WebSocketDistributionEvent(
    String eventId,
    Long roomId,
    Long senderId,
    String content,
    Instant createdAt,
    MessageType messageType
) {
    public enum MessageType {
        CHAT_MESSAGE,
        ROOM_JOIN,
        ROOM_LEAVE,
        USER_TYPING
    }

    public WebSocketDistributionEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
    }

    public static WebSocketDistributionEvent chatMessage(Long roomId, Long senderId, String content, Instant createdAt) {
        return new WebSocketDistributionEvent(null, roomId, senderId, content, createdAt, MessageType.CHAT_MESSAGE);
    }

    public static WebSocketDistributionEvent roomJoin(Long roomId, Long userId) {
        return new WebSocketDistributionEvent(null, roomId, userId, null, Instant.now(), MessageType.ROOM_JOIN);
    }

    public static WebSocketDistributionEvent roomLeave(Long roomId, Long userId) {
        return new WebSocketDistributionEvent(null, roomId, userId, null, Instant.now(), MessageType.ROOM_LEAVE);
    }
}
