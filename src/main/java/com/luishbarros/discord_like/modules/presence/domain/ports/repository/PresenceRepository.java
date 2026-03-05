package com.luishbarros.discord_like.modules.presence.domain.ports.repository;

import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;

import java.util.Optional;
import java.util.Set;

public interface PresenceRepository {
    UserPresence save(UserPresence presence);
    Optional<UserPresence> findByUserId(Long userId);
    Set<UserPresence> findByState(String state);
    Set<Long> getOnlineUserIds();
    void delete(UserPresence presence);
}
