package com.luishbarros.discord_like.modules.presence.domain.event;

import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

import java.time.Instant;
import java.util.UUID;

public record PresenceEvents(
        String eventId,
        Long userId,
        PresenceState state,
        Instant occurredAt,
        EventType type
) {

    public enum EventType {
        USER_CAME_ONLINE,
        USER_WENT_OFFLINE,
        USER_STATE_CHANGED
    }

    public PresenceEvents {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
    }

    public static PresenceEvents userCameOnline(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
                UUID.randomUUID().toString(),
                presence.getUserId(),
                presence.getState(),
                occurredAt,
                EventType.USER_CAME_ONLINE
        );
    }

    public static PresenceEvents userWentOffline(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
                UUID.randomUUID().toString(),
                presence.getUserId(),
                presence.getState(),
                occurredAt,
                EventType.USER_WENT_OFFLINE
        );
    }

    public static PresenceEvents userStateChanged(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
                UUID.randomUUID().toString(),
                presence.getUserId(),
                presence.getState(),
                occurredAt,
                EventType.USER_STATE_CHANGED
        );
    }
}
