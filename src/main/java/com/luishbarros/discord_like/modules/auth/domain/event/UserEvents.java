package com.luishbarros.discord_like.modules.auth.domain.event;

import com.luishbarros.discord_like.modules.auth.domain.model.User;

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
                user.getUsername(),
                user.getEmail(),
                occurredAt,
                EventType.REGISTERED
        );
    }

    public static UserEvents passwordChanged(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                occurredAt,
                EventType.PASSWORD_CHANGED
        );
    }

    public static UserEvents deactivated(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                occurredAt,
                EventType.DEACTIVATED
        );
    }

    public static UserEvents activated(User user, Instant occurredAt) {
        return new UserEvents(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                occurredAt,
                EventType.ACTIVATED
        );
    }
}