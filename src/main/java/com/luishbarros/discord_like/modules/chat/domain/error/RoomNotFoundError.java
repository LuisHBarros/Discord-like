package com.luishbarros.discord_like.modules.chat.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class RoomNotFoundError extends DomainError {

    public RoomNotFoundError(String roomId) {
        super("ROOM_NOT_FOUND", "Room not found: " + roomId);
    }
}
