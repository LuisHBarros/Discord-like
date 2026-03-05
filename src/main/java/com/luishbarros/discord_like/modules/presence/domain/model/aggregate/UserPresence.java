package com.luishbarros.discord_like.modules.presence.domain.model.aggregate;

import com.luishbarros.discord_like.modules.presence.domain.model.value_object.LastSeen;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

public class UserPresence extends BaseEntity {

    private Long userId;
    private PresenceState state;
    private LastSeen lastSeen;

    protected UserPresence() {}

    public UserPresence(Long userId, PresenceState state, Instant createdAt) {
        this.userId = userId;
        this.state = state;
        this.lastSeen = new LastSeen(createdAt);
    }

    public static UserPresence reconstitute(Long id, Long userId, PresenceState state, LastSeen lastSeen) {
        UserPresence presence = new UserPresence();
        presence.id = id;
        presence.userId = userId;
        presence.state = state;
        presence.lastSeen = lastSeen;
        return presence;
    }

    public void setOnline() {
        this.state = PresenceState.ONLINE;
        this.lastSeen = LastSeen.now();
    }

    public void setOffline() {
        this.state = PresenceState.OFFLINE;
        this.lastSeen = LastSeen.now();
    }

    public void setState(PresenceState newState) {
        this.state = newState;
        this.lastSeen = LastSeen.now();
    }

    public void updateLastActivity() {
        this.lastSeen = LastSeen.now();
    }

    public Long getUserId() {
        return userId;
    }

    public PresenceState getState() {
        return state;
    }

    public LastSeen getLastSeen() {
        return lastSeen;
    }
}
