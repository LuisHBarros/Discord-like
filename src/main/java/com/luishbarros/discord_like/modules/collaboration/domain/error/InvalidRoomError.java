package com.luishbarros.discord_like.modules.collaboration.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidRoomError extends DomainError {

    public InvalidRoomError(String message) {
        super("INVALID_ROOM", message);
    }

    public static InvalidRoomError invalidName(String message) {
        return new InvalidRoomError("Invalid room name: " + message);
    }

}
