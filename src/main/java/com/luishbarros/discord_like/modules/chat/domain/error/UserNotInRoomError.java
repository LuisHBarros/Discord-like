package com.luishbarros.discord_like.modules.chat.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class UserNotInRoomError extends DomainError {

    public UserNotInRoomError(String userId, String roomId) {
        super("USER_NOT_IN_ROOM", "User " + userId + " is not a member of room " + roomId);
    }
}
