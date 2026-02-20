package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
public class InviteJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "code_value", nullable = false, unique = true)
    private String codeValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected InviteJpaEntity() {}

    public InviteJpaEntity(UUID id, UUID roomId, UUID createdByUserId, String codeValue, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.roomId = roomId;
        this.createdByUserId = createdByUserId;
        this.codeValue = codeValue;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public String getCodeValue() { return codeValue; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    // Mapping to domain model
    public com.luishbarros.discord_like.modules.chat.domain.model.Invite toDomain() {
        com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode code =
                new com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode(
                        this.codeValue,
                        this.createdByUserId
                );

        return new com.luishbarros.discord_like.modules.chat.domain.model.Invite(
                this.roomId,
                this.createdByUserId,
                code,
                this.createdAt
        );
    }

    // Mapping from domain model
    public static InviteJpaEntity fromDomain(com.luishbarros.discord_like.modules.chat.domain.model.Invite invite) {
        return new InviteJpaEntity(
                invite.getId(),
                invite.getRoomId(),
                invite.getCreatedByUserId(),
                invite.getCode().value(),
                invite.getCreatedAt(),
                invite.getExpiresAt()
        );
    }
}