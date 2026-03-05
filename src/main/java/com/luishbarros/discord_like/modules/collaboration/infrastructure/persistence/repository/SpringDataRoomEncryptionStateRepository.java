package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomEncryptionStateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataRoomEncryptionStateRepository
        extends JpaRepository<RoomEncryptionStateJpaEntity, Long> {

    Optional<RoomEncryptionStateJpaEntity> findByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}
