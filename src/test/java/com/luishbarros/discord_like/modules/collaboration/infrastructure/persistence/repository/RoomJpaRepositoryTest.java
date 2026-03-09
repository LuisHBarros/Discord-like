package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomJpaRepositoryTest {

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @BeforeEach
    void setUp() {
        roomJpaRepository.deleteAll();
    }

    @Test
    void savesAndRetrievesRoom() {
        Instant now = Instant.now();
        RoomJpaEntity room = new RoomJpaEntity(null, "General", 10L, now, now);
        
        RoomJpaEntity saved = roomJpaRepository.save(room);
        
        Optional<RoomJpaEntity> retrieved = roomJpaRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("General");
        assertThat(retrieved.get().getOwnerId()).isEqualTo(10L);
    }
}
