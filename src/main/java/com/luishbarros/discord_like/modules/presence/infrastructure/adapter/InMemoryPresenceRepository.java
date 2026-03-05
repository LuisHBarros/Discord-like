package com.luishbarros.discord_like.modules.presence.infrastructure.adapter;

import com.luishbarros.discord_like.modules.presence.domain.ports.repository.PresenceRepository;
import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryPresenceRepository implements PresenceRepository {

    private final Map<Long, UserPresence> store = new ConcurrentHashMap<>();

    @Override
    public UserPresence save(UserPresence presence) {
        store.put(presence.getUserId(), presence);
        return presence;
    }

    @Override
    public Optional<UserPresence> findByUserId(Long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Set<UserPresence> findByState(String state) {
        return store.values().stream()
            .filter(p -> p.getState().name().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        return store.values().stream()
            .filter(p -> p.getState().isOnline())
            .map(UserPresence::getUserId)
            .collect(Collectors.toSet());
    }

    @Override
    public void delete(UserPresence presence) {
        store.remove(presence.getUserId());
    }

    public void clear() {
        store.clear();
    }
}
