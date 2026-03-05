package com.luishbarros.discord_like.modules.presence.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidPresenceError extends DomainError {
    public InvalidPresenceError(String message) {
        super("INVALID_PRESENCE", message);
    }

    public static InvalidPresenceError invalidState(String message) {
        return new InvalidPresenceError(message);
    }
}
