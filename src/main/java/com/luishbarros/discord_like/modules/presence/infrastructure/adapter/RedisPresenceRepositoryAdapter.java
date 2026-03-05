package com.luishbarros.discord_like.modules.presence.infrastructure.adapter;

import com.luishbarros.discord_like.modules.presence.domain.ports.repository.PresenceRepository;
import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.LastSeen;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RedisPresenceRepositoryAdapter implements PresenceRepository {

    private static final String ONLINE_KEY = "presence:online";
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisPresenceRepositoryAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserPresence save(UserPresence presence) {
        String userIdKey = PRESENCE_KEY_PREFIX + presence.getUserId();

        // Store presence data as hash
        Map<String, String> presenceData = new HashMap<>();
        presenceData.put("userId", String.valueOf(presence.getUserId()));
        presenceData.put("state", presence.getState().name());
        presenceData.put("lastSeen", presence.getLastSeen().timestamp().toString());

        redisTemplate.opsForHash().putAll(userIdKey, presenceData);

        // Add to online set if online
        if (presence.getState().isOnline()) {
            redisTemplate.opsForSet().add(ONLINE_KEY, String.valueOf(presence.getUserId()));
        } else {
            redisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(presence.getUserId()));
        }

        return presence;
    }

    @Override
    public Optional<UserPresence> findByUserId(Long userId) {
        String userIdKey = PRESENCE_KEY_PREFIX + userId;
        Map<Object, Object> presenceData = redisTemplate.opsForHash().entries(userIdKey);

        if (presenceData == null || presenceData.isEmpty()) {
            return Optional.empty();
        }

        Long foundUserId = Long.parseLong((String) presenceData.get("userId"));
        PresenceState state = PresenceState.valueOf((String) presenceData.get("state"));
        Instant lastSeen = Instant.parse((String) presenceData.get("lastSeen"));

        UserPresence presence = UserPresence.reconstitute(null, foundUserId, state, new LastSeen(lastSeen));
        return Optional.of(presence);
    }

    @Override
    public Set<UserPresence> findByState(String state) {
        // This is inefficient for Redis, but we'll iterate through all presence data
        // In a production system, you'd want a different data structure
        Set<UserPresence> result = new HashSet<>();
        Set<String> allKeys = redisTemplate.keys(PRESENCE_KEY_PREFIX + "*");

        if (allKeys != null) {
            for (String key : allKeys) {
                Map<Object, Object> presenceData = redisTemplate.opsForHash().entries(key);
                if (presenceData != null && !presenceData.isEmpty()) {
                    PresenceState presenceState = PresenceState.valueOf((String) presenceData.get("state"));
                    if (presenceState.name().equals(state)) {
                        Long userId = Long.parseLong((String) presenceData.get("userId"));
                        Instant lastSeen = Instant.parse((String) presenceData.get("lastSeen"));
                        UserPresence presence = UserPresence.reconstitute(null, userId, presenceState, new LastSeen(lastSeen));
                        result.add(presence);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        Set<String> members = redisTemplate.opsForSet().members(ONLINE_KEY);
        if (members == null) return Set.of();
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    @Override
    public void delete(UserPresence presence) {
        String userIdKey = PRESENCE_KEY_PREFIX + presence.getUserId();
        redisTemplate.delete(userIdKey);
        redisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(presence.getUserId()));
    }
}
