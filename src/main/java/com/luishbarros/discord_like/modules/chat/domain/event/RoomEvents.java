package com.luishbarros.discord_like.modules.chat.domain.event;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.time.Instant;
import java.util.UUID;

public record RoomEvents(
        UUID roomId,
        UUID userId,
        Instant occurredAt,
        EventType type
) {

    public enum EventType {
        ROOM_CREATED,
        MEMBER_JOINED,
        MEMBER_LEFT,
        ROOM_UPDATED
    }

    public static RoomEvents roomCreated(Room room, Instant occurredAt) {
        return new RoomEvents(
                room.getId(),
                null,
                occurredAt,
                EventType.ROOM_CREATED
        );
    }

    public static RoomEvents memberJoined(UUID roomId, UUID userId, Instant occurredAt) {
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.MEMBER_JOINED
        );
    }

    public static RoomEvents memberLeft(UUID roomId, UUID userId, Instant occurredAt) {
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.MEMBER_LEFT
        );
    }
    public static RoomEvents roomUpdated(UUID roomId, UUID userId, Instant occurredAt){
        return new RoomEvents(
                roomId,
                userId,
                occurredAt,
                EventType.ROOM_UPDATED
        );
    }
}
