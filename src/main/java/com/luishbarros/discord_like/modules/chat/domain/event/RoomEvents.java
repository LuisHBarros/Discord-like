package com.luishbarros.discord_like.modules.chat.domain.event;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.time.Instant;
import java.util.UUID;

public record RoomEvents(
        Long roomId,
        Long userId,
        Instant occurredAt,
        EventType type
) {
    public enum EventType {
        ROOM_CREATED,
        MEMBER_JOINED,
        MEMBER_LEFT,
        ROOM_UPDATED,
        ROOM_DELETED
    }

    public static RoomEvents roomCreated(Room room, Instant occurredAt) {
        return new RoomEvents(
                room.getId(),
                null,
                occurredAt,
                EventType.ROOM_CREATED
        );
    }

    public static RoomEvents memberJoined(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.MEMBER_JOINED
        );
    }

    public static RoomEvents memberLeft(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.MEMBER_LEFT
        );
    }
    public static RoomEvents roomUpdated(Long roomId, Long userId, Instant occurredAt){
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.ROOM_UPDATED
        );
    }
    public static RoomEvents roomDeleted(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.ROOM_DELETED
        );
    }
}
