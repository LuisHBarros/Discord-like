package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.chat.domain.model.Invite;
import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invites")
public class InviteJpaEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "code_value", nullable = false, unique = true)
    private String codeValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected InviteJpaEntity() {}

    public InviteJpaEntity(Long id, Long roomId, Long createdByUserId, String codeValue, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.roomId = roomId;
        this.createdByUserId = createdByUserId;
        this.codeValue = codeValue;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public String getCodeValue() { return codeValue; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    // Mapping to domain model
    public Invite toDomain() {
        InviteCode code = new InviteCode(this.codeValue, this.createdByUserId);
        return Invite.reconstitute(
                this.id,
                this.roomId,
                this.createdByUserId,
                code,
                this.createdAt,
                this.expiresAt   // <-- preserva o expiresAt real do banco
        );
    }

    // Mapping from domain model
    public static InviteJpaEntity fromDomain(Invite invite) {
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