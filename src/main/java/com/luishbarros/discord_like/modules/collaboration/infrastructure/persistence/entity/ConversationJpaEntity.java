package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for persisting Conversation aggregates.
 * Represents the conversations table in the database.
 */
@Entity
@Table(name = "conversations")
public class ConversationJpaEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // One-to-many relationship with messages
    // Note: This is for ORM mapping, but messages are typically loaded separately
    // for performance reasons in a chat application
    @OneToMany(mappedBy = "conversationId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageJpaEntity> messages = new ArrayList<>();

    protected ConversationJpaEntity() {}

    public ConversationJpaEntity(Long id, Long roomId, Instant lastActivityAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.roomId = roomId;
        this.lastActivityAt = lastActivityAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<MessageJpaEntity> getMessages() { return messages; }

    // Setters
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setMessages(List<MessageJpaEntity> messages) { this.messages = messages; }

    // Mapping to domain model
    public com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation toDomain(List<Message> messageList) {
        java.util.SortedSet<Message> messagesSet = new java.util.TreeSet<>(
            (m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())
        );
        messagesSet.addAll(messageList);

        return com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation.reconstitute(
            this.id,
            this.roomId,
            messagesSet,
            this.lastActivityAt
        );
    }

    // Mapping from domain model
    public static ConversationJpaEntity fromDomain(com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation conversation) {
        Instant now = Instant.now();
        ConversationJpaEntity entity = new ConversationJpaEntity(
            conversation.getId(),
            conversation.getRoomId(),
            conversation.getLastActivityAt(),
            conversation.getCreatedAt() != null ? conversation.getCreatedAt() : now,
            conversation.getUpdatedAt() != null ? conversation.getUpdatedAt() : now
        );
        return entity;
    }
}
