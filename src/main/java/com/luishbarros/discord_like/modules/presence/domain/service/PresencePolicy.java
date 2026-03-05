package com.luishbarros.discord_like.modules.presence.domain.service;

import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

import java.time.Duration;
import java.time.Instant;

public interface PresencePolicy {
    boolean canTransitionTo(PresenceState from, PresenceState to);
    Duration getAutoAwayTimeout();
    Duration getOfflineTimeout();
    PresenceState inferStateFromActivity(Instant lastActivity);
}
