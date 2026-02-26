package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class MessageJpaEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "ciphertext", nullable = false, columnDefinition = "TEXT")
    private String ciphertext;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    protected MessageJpaEntity() {}

    public MessageJpaEntity(Long id, Long senderId, Long roomId, String ciphertext, Instant createdAt, Instant editedAt) {
        this.id = id;
        this.senderId = senderId;
        this.roomId = roomId;
        this.ciphertext = ciphertext;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getRoomId() { return roomId; }
    public String getCiphertext() { return ciphertext; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }

    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    // Mapping to domain model
    public com.luishbarros.discord_like.modules.chat.domain.model.Message toDomain() {
        return com.luishbarros.discord_like.modules.chat.domain.model.Message.reconstitute(
                this.id,
                this.senderId,
                this.roomId,
                this.ciphertext,
                this.createdAt,
                this.editedAt
        );
    }

    // Mapping from domain model
    public static MessageJpaEntity fromDomain(com.luishbarros.discord_like.modules.chat.domain.model.Message message) {
        return new MessageJpaEntity(
                message.getId(),
                message.getSenderId(),
                message.getRoomId(),
                message.getCiphertext(),
                message.getCreatedAt(),
                message.getEditedAt()
        );
    }
}