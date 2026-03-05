package com.luishbarros.discord_like.modules.presence.domain.service;

import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.Duration;

@Service
public class DefaultPresencePolicy implements PresencePolicy {

    private static final Duration AUTO_AWAY_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration OFFLINE_TIMEOUT = Duration.ofMinutes(10);

    @Override
    public boolean canTransitionTo(PresenceState from, PresenceState to) {
        // Define valid state transitions
        return switch (from) {
            case ONLINE -> to == PresenceState.AWAY || to == PresenceState.BUSY || to == PresenceState.OFFLINE;
            case AWAY -> to == PresenceState.ONLINE || to == PresenceState.BUSY || to == PresenceState.OFFLINE;
            case BUSY -> to == PresenceState.ONLINE || to == PresenceState.AWAY || to == PresenceState.OFFLINE;
            case OFFLINE -> to == PresenceState.ONLINE || to == PresenceState.AWAY || to == PresenceState.BUSY;
            case INVISIBLE -> to == PresenceState.ONLINE || to == PresenceState.AWAY || to == PresenceState.BUSY || to == PresenceState.OFFLINE;
        };
    }

    @Override
    public Duration getAutoAwayTimeout() {
        return AUTO_AWAY_TIMEOUT;
    }

    @Override
    public Duration getOfflineTimeout() {
        return OFFLINE_TIMEOUT;
    }

    @Override
    public PresenceState inferStateFromActivity(Instant lastActivity) {
        if (lastActivity == null) {
            return PresenceState.OFFLINE;
        }

        Duration inactive = Duration.between(lastActivity, Instant.now());

        if (inactive.compareTo(OFFLINE_TIMEOUT) >= 0) {
            return PresenceState.OFFLINE;
        } else if (inactive.compareTo(AUTO_AWAY_TIMEOUT) >= 0) {
            return PresenceState.AWAY;
        }

        return PresenceState.ONLINE;
    }
}
