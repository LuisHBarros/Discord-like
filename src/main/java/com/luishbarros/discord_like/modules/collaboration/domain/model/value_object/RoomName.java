package com.luishbarros.discord_like.modules.collaboration.domain.model.value_object;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidRoomError;

public record RoomName(String value) {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public RoomName {
        if (value == null || value.isBlank()) {
            throw InvalidRoomError.invalidName("Room name cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw InvalidRoomError.invalidName("Room name cannot exceed " + MAX_LENGTH + " characters");
        }
    }
}
