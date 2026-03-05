package com.luishbarros.discord_like.shared.adapters.cache;

import com.luishbarros.discord_like.shared.domain.event.CacheInvalidationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @KafkaListener(
            topics = "cache-invalidation-events",
            groupId = "discord-like-cache-invalidation"
    )
    public void handleInvalidation(CacheInvalidationEvent event) {
        try {
            Cache cache = cacheManager.getCache(event.cacheName());
            if (cache == null) {
                log.warn("Cache not found for invalidation: {}", event.cacheName());
                return;
            }

            if (event.allEntries()) {
                log.info("Evicting all entries from cache: {}", event.cacheName());
                cache.clear();
            } else {
                event.keys().forEach(key -> {
                    log.debug("Evicting key {} from cache {}", key, event.cacheName());
                    cache.evictIfPresent(key);
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle cache invalidation event: {}", event, e);
        }
    }
}
