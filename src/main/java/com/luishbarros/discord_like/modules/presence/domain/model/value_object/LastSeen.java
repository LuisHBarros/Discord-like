package com.luishbarros.discord_like.modules.presence.domain.model.value_object;

import java.time.Duration;
import java.time.Instant;

public record LastSeen(Instant timestamp) {
    public LastSeen {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static LastSeen now() {
        return new LastSeen(Instant.now());
    }

    public boolean isOlderThan(Duration duration) {
        return timestamp.isBefore(Instant.now().minus(duration));
    }
}
