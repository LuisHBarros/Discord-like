package com.luishbarros.discord_like.modules.identity.domain.event;

import com.luishbarros.discord_like.modules.identity.domain.model.User;

import java.time.Instant;

public record UserEvents(
        Long userId,
        String username,
        String email,
        Instant occurredAt,
        EventType type
) {

    public enum EventType {
        REGISTERED,
        PASSWORD_CHANGED,
        DEACTIVATED,
        ACTIVATED
    }

    public static UserEvents registered(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                occurredAt,
                EventType.REGISTERED
        );
    }

    public static UserEvents passwordChanged(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                occurredAt,
                EventType.PASSWORD_CHANGED
        );
    }

    public static UserEvents deactivated(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                occurredAt,
                EventType.DEACTIVATED
        );
    }

    public static UserEvents activated(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                occurredAt,
                EventType.ACTIVATED
        );
    }
}