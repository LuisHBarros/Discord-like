// shared/adapters/presence/RedisPresenceStore.java
package com.luishbarros.discord_like.shared.adapters.presence;

import com.luishbarros.discord_like.shared.ports.PresenceStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RedisPresenceStore implements PresenceStore {

    private static final String KEY = "presence:online";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisPresenceStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setOnline(Long userId) {
        redisTemplate.opsForSet().add(KEY, String.valueOf(userId));
    }

    @Override
    public void setOffline(Long userId) {
        redisTemplate.opsForSet().remove(KEY, String.valueOf(userId));
    }

    @Override
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(KEY, String.valueOf(userId))
        );
    }

    @Override
    public Set<Long> getOnlineUsers() {
        Set<String> members = redisTemplate.opsForSet().members(KEY);
        if (members == null) return Set.of();
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
    }
}
