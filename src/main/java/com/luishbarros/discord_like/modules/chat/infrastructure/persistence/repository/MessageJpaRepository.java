package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.MessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {

    // Custom query to find messages in a room created before the timestamp of a specific message
    @Query("SELECT m FROM MessageJpaEntity m WHERE m.roomId = :roomId " +
            "AND m.createdAt < (SELECT m2.createdAt FROM MessageJpaEntity m2 WHERE m2.id = :beforeId) " +
            "ORDER BY m.createdAt DESC")
    List<MessageJpaEntity> findByRoomIdAndIdBefore(Long roomId, Long beforeId, Pageable pageable);

    List<MessageJpaEntity> findByRoomId(Long roomId);

    List<MessageJpaEntity> findByRoomId(Long roomId, Pageable pageable);

    Optional<MessageJpaEntity> findByIdAndRoomId(Long id, Long roomId);

    void deleteByRoomId(Long roomId);

    long countByRoomId(Long roomId);
}
