package com.luishbarros.discord_like.modules.chat.application.dto;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        UUID ownerId,
        Set<UUID> memberIds,
        Instant createdAt
) {
    public static RoomResponse fromRoom(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwnerId(),
                room.getMemberIds(),
                room.getCreatedAt()
        );
    }
}
