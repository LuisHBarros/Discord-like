package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "room_memberships", indexes = {
        @Index(name = "idx_room_memberships_room_user", columnList = "room_id, user_id", unique = true),
        @Index(name = "idx_room_memberships_user", columnList = "user_id")
})
public class RoomMembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "encrypted_room_key", columnDefinition = "BYTEA")
    private byte[] encryptedRoomKey;

    protected RoomMembershipJpaEntity() {}

    public RoomMembershipJpaEntity(Long id, Long roomId, Long userId, Instant joinedAt, byte[] encryptedRoomKey) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.encryptedRoomKey = encryptedRoomKey;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Long getUserId() { return userId; }
    public Instant getJoinedAt() { return joinedAt; }
    public byte[] getEncryptedRoomKey() { return encryptedRoomKey; }

    public void setEncryptedRoomKey(byte[] encryptedRoomKey) {
        this.encryptedRoomKey = encryptedRoomKey;
    }

    // Mapping to domain model
    public RoomMembership toDomain() {
        return RoomMembership.reconstitute(
                this.id,
                this.roomId,
                this.userId,
                this.joinedAt,
                this.encryptedRoomKey
        );
    }

    // Mapping from domain model
    public static RoomMembershipJpaEntity fromDomain(RoomMembership membership) {
        return new RoomMembershipJpaEntity(
                membership.getId(),
                membership.getRoomId(),
                membership.getUserId(),
                membership.getJoinedAt(),
                membership.getEncryptedRoomKey()
        );
    }
}
