package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.RoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

    @Query("SELECT r FROM RoomJpaEntity r WHERE :memberId MEMBER OF r.memberIds")
    List<RoomJpaEntity> findByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT r FROM RoomJpaEntity r JOIN InviteJpaEntity i ON i.roomId = r.id WHERE i.codeValue = :inviteCode")
    Optional<RoomJpaEntity> findByInviteCode(@Param("inviteCode") String inviteCode);
}
