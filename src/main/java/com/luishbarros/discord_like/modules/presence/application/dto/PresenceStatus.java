package com.luishbarros.discord_like.modules.presence.application.dto;

import java.time.Instant;

public record PresenceStatus(
    Long userId,
    String state,
    Instant lastSeenAt,
    String username
) {
    public static PresenceStatus fromDomain(
        Long userId,
        String state,
        Instant lastSeenAt,
        String username
    ) {
        return new PresenceStatus(userId, state, lastSeenAt, username);
    }
}
