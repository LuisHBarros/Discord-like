package com.luishbarros.discord_like.modules.collaboration.application.dto;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;

import java.time.Instant;
import java.util.Set;

public record RoomResponse(
        Long id,
        String name,
        Long ownerId,
        Set<Long> memberIds,
        Instant createdAt
) {
    public static RoomResponse fromRoom(Room room, Set<Long> memberIds) {
        return new RoomResponse(
                room.getId(),
                room.getName().value(),
                room.getOwnerId(),
                memberIds,
                room.getCreatedAt()
        );
    }
}
