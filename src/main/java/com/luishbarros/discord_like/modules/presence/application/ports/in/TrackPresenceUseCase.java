package com.luishbarros.discord_like.modules.presence.application.ports.in;

import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

public interface TrackPresenceUseCase {
    void setOnline(Long userId);
    void setOffline(Long userId);
    void setPresenceState(Long userId, PresenceState state);
    void updateLastActivity(Long userId);
}
