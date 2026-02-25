// auth/infrastructure/security/InMemoryTokenBlacklist.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    @Override
    public void add(String token) {
        blacklist.add(token);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}