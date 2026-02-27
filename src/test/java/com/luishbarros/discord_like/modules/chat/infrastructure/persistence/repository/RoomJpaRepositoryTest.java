package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.RoomJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RoomJpaRepositoryTest {

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Test
    void findByMemberIdReturnsRoomsWhereMemberExists() {
        Instant now = Instant.now();
        RoomJpaEntity room1 = new RoomJpaEntity(null, "General", 10L, Set.of(1L, 2L), now, now);
        RoomJpaEntity room2 = new RoomJpaEntity(null, "Backend", 11L, Set.of(3L, 4L), now, now);
        RoomJpaEntity room3 = new RoomJpaEntity(null, "Frontend", 12L, Set.of(1L, 5L), now, now);
        roomJpaRepository.saveAll(List.of(room1, room2, room3));

        List<RoomJpaEntity> result = roomJpaRepository.findByMemberId(1L);

        assertThat(result)
                .extracting(RoomJpaEntity::getName)
                .containsExactlyInAnyOrder("General", "Frontend");
    }

    @Test
    void findByMemberIdReturnsEmptyWhenMemberDoesNotExist() {
        Instant now = Instant.now();
        RoomJpaEntity room = new RoomJpaEntity(null, "General", 10L, Set.of(2L, 3L), now, now);
        roomJpaRepository.save(room);

        List<RoomJpaEntity> result = roomJpaRepository.findByMemberId(99L);

        assertThat(result).isEmpty();
    }
}
