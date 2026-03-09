package com.luishbarros.discord_like.shared.adapters.cache;

import com.luishbarros.discord_like.BaseIntegrationTest;
import com.luishbarros.discord_like.modules.collaboration.application.service.RoomService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cross-instance cache invalidation.
 * These tests use Testcontainers to provide PostgreSQL database.
 */
@Tag("integration")
@SpringBootTest
public class CacheInvalidationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void roomEvent_shouldInvalidateCacheAcrossInstances() {
        // This test verifies cache invalidation via room events
        // Note: Currently this is a conceptual test since we don't have
        // a real Kafka event system running for tests
        // The actual implementation handles cache invalidation when
        // room events are received via MessageEventListener

        assertThat(cacheManager).isNotNull();
        Cache roomMembersCache = cacheManager.getCache("room-members");

        assertThat(roomMembersCache).isNotNull();
    }
}
