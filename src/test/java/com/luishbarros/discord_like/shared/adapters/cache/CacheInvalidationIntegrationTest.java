package com.luishbarros.discord_like.shared.adapters.cache;

import com.luishbarros.discord_like.modules.collaboration.application.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class CacheInvalidationIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void roomEvent_shouldInvalidateCacheAcrossInstances() {
        // Test cache invalidation via Kafka events
        Long roomId = 1L;
        Long userId = 2L;
        Cache roomMembersCache = cacheManager.getCache("room-members");

        // Populate cache
        roomService.getMembers(roomId, 1L);

        // Add member - publishes event
        roomService.addMember(roomId, userId, Instant.now());

        // Verify cache eviction via Kafka
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Cache.ValueWrapper value = roomMembersCache.get(roomId);
                    assertThat(value).isNull();
                });
    }
}
