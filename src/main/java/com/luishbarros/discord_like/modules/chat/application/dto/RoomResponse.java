package com.luishbarros.discord_like.modules.chat.application.dto;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.time.Instant;
import java.util.Set;

public record RoomResponse(
        Long id,
        String name,
        Long ownerId,
        Set<Long> memberIds,
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
