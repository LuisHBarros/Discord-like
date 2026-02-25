// auth/infrastructure/security/RedisTokenBlacklist.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Primary
@Component
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String PREFIX = "blacklist:";
    private static final long TTL_DAYS = 7;

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenBlacklist(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String token) {
        redisTemplate.opsForValue().set(
                PREFIX + token,
                "true",
                Duration.ofDays(TTL_DAYS)
        );
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}