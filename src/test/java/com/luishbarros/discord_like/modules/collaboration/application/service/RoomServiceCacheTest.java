package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RoomServiceCacheTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void findByMemberId_shouldCacheResult() {
        Long userId = 1L;
        Cache userRoomsCache = cacheManager.getCache("user-rooms");

        // First call - cache miss
        List<Room> firstCall = roomService.findByMemberId(userId);

        // Verify cache
        Cache.ValueWrapper cachedValue = userRoomsCache.get(userId);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(firstCall);
    }

    @Test
    void addMember_shouldEvictCache() {
        Long roomId = 1L;
        Long userId = 2L;
        Cache roomMembersCache = cacheManager.getCache("room-members");

        // Populate cache
        roomService.getMembers(roomId, 1L);
        assertThat(roomMembersCache.get(roomId)).isNotNull();

        // Add member
        roomService.addMember(roomId, userId, Instant.now());

        // Verify cache eviction
        assertThat(roomMembersCache.get(roomId)).isNull();
    }
}
