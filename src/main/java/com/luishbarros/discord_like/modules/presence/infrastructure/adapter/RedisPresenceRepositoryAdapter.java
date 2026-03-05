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
    private static final String STATE_KEY_PREFIX = "presence:state:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisPresenceRepositoryAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserPresence save(UserPresence presence) {
        String userIdKey = PRESENCE_KEY_PREFIX + presence.getUserId();
        String stateKey = STATE_KEY_PREFIX + presence.getState().name();

        // Store presence data as hash
        Map<String, String> presenceData = new HashMap<>();
        presenceData.put("userId", String.valueOf(presence.getUserId()));
        presenceData.put("state", presence.getState().name());
        presenceData.put("lastSeen", presence.getLastSeen().timestamp().toString());

        redisTemplate.opsForHash().putAll(userIdKey, presenceData);

        // Add to state-specific set for efficient querying
        redisTemplate.opsForSet().add(stateKey, String.valueOf(presence.getUserId()));

        // Add to online set if online
        if (presence.getState().isOnline()) {
            redisTemplate.opsForSet().add(ONLINE_KEY, String.valueOf(presence.getUserId()));
        } else {
            redisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(presence.getUserId()));
        }

        // Remove from all other state sets
        for (com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState state :
                com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState.values()) {
            if (!state.equals(presence.getState())) {
                redisTemplate.opsForSet().remove(STATE_KEY_PREFIX + state.name(), String.valueOf(presence.getUserId()));
            }
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
        Set<UserPresence> result = new HashSet<>();
        PresenceState presenceState = PresenceState.valueOf(state);

        // Use state-specific set for efficient querying
        String stateKey = STATE_KEY_PREFIX + state;
        Set<String> userIds = redisTemplate.opsForSet().members(stateKey);

        if (userIds != null && !userIds.isEmpty()) {
            for (String userIdStr : userIds) {
                Long userId = Long.parseLong(userIdStr);
                Optional<UserPresence> presenceOpt = findByUserId(userId);
                presenceOpt.ifPresent(presence -> {
                    // Double-check the state to ensure consistency
                    if (presence.getState().equals(presenceState)) {
                        result.add(presence);
                    }
                });
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
        String stateKey = STATE_KEY_PREFIX + presence.getState().name();

        redisTemplate.delete(userIdKey);
        redisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(presence.getUserId()));

        // Remove from state-specific set
        redisTemplate.opsForSet().remove(stateKey, String.valueOf(presence.getUserId()));
    }
}
