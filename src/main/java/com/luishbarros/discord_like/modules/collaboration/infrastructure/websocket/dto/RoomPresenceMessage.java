package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto;

import java.time.Instant;

public record RoomPresenceMessage(
        String type,
        Long roomId,
        Long userId,
        Instant timestamp
) {
    public static RoomPresenceMessage joined(Long roomId, Long userId, Instant timestamp) {
        return new RoomPresenceMessage("room_joined", roomId, userId, timestamp);
    }

    public static RoomPresenceMessage left(Long roomId, Long userId, Instant timestamp) {
        return new RoomPresenceMessage("room_left", roomId, userId, timestamp);
    }
}
