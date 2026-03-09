package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomMembershipJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMembershipJpaRepository extends JpaRepository<RoomMembershipJpaEntity, Long> {

    Optional<RoomMembershipJpaEntity> findByRoomIdAndUserId(Long roomId, Long userId);

    List<RoomMembershipJpaEntity> findByRoomId(Long roomId);

    List<RoomMembershipJpaEntity> findByUserId(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    void deleteByRoomIdAndUserId(Long roomId, Long userId);
    
    void deleteByRoomId(Long roomId);
}
