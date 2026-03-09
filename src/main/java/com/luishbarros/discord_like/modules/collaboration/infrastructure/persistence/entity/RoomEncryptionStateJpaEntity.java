package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "room_encryption_states")
public class RoomEncryptionStateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "encryption_mode", nullable = false)
    private RoomEncryptionState.EncryptionMode mode;

    @Column(name = "key_rotated_at")
    private Instant keyRotatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoomEncryptionStateJpaEntity() {
    }

    public RoomEncryptionStateJpaEntity(Long roomId, RoomEncryptionState.EncryptionMode mode,
            Instant keyRotatedAt, Instant createdAt) {
        this.roomId = roomId;
        this.mode = mode;
        this.keyRotatedAt = keyRotatedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public RoomEncryptionState.EncryptionMode getMode() {
        return mode;
    }

    public Instant getKeyRotatedAt() {
        return keyRotatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Mapping to domain model
    public RoomEncryptionState toDomain() {
        return RoomEncryptionState.reconstitute(
                this.id,
                this.roomId,
                this.mode,
                this.keyRotatedAt,
                this.createdAt);
    }

    // Mapping from domain model
    public static RoomEncryptionStateJpaEntity fromDomain(RoomEncryptionState state) {
        RoomEncryptionStateJpaEntity entity = new RoomEncryptionStateJpaEntity(
                state.roomId(),
                state.mode(),
                state.keyRotatedAt(),
                state.createdAt());
        entity.id = state.getId();
        return entity;
    }
}
