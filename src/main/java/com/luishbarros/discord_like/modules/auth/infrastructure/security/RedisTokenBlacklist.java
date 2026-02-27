// auth/infrastructure/security/RedisTokenBlacklist.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Primary
@Component
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String PREFIX = "blacklist:";

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenProvider tokenProvider;

    public RedisTokenBlacklist(RedisTemplate<String, String> redisTemplate, TokenProvider tokenProvider) {
        this.redisTemplate = redisTemplate;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void add(String token) {
        Instant expiry = tokenProvider.getInspiration(token);
        Instant now = Instant.now();
        Duration ttl = Duration.between(now, expiry);
        if(ttl.isNegative() || ttl.isZero()) return;
        redisTemplate.opsForValue().set(
                PREFIX + token,
                "true",
                ttl
        );
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}