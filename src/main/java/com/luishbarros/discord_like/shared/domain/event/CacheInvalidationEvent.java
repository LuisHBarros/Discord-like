package com.luishbarros.discord_like.shared.domain.event;

import java.time.Instant;
import java.util.Set;

public record CacheInvalidationEvent(
        String cacheName,
        Set<String> keys,
        boolean allEntries,
        Instant occurredAt
) {
    public static CacheInvalidationEvent evictKeys(String cacheName, Set<String> keys) {
        return new CacheInvalidationEvent(cacheName, keys, false, Instant.now());
    }

    public static CacheInvalidationEvent evictAll(String cacheName) {
        return new CacheInvalidationEvent(cacheName, Set.of(), true, Instant.now());
    }
}
