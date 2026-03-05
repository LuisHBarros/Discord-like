package com.luishbarros.discord_like.modules.presence.domain.model.value_object;

import com.luishbarros.discord_like.modules.presence.domain.model.error.InvalidPresenceError;

public enum PresenceState {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    INVISIBLE;

    public boolean isOnline() {
        return this == ONLINE;
    }

    public static PresenceState fromString(String state) {
        if (state == null || state.isBlank()) {
            return OFFLINE;
        }
        try {
            return PresenceState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidPresenceError.invalidState("Invalid presence state: " + state);
        }
    }
}
