package com.luishbarros.discord_like.modules.chat.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidRoomError extends DomainError {

    public InvalidRoomError(String message) {
        super("INVALID_ROOM", message);
    }

}
