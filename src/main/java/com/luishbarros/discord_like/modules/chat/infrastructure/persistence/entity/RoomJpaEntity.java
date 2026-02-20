package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class RoomJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ElementCollection
    @CollectionTable(name = "room_members", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "user_id", nullable = false)
    private Set<UUID> memberIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomJpaEntity() {}

    public RoomJpaEntity(UUID id, String name, UUID ownerId, Set<UUID> memberIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds = new HashSet<>(memberIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public Set<UUID> getMemberIds() { return Set.copyOf(memberIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Mapping to domain model
    public com.luishbarros.discord_like.modules.chat.domain.model.Room toDomain() {
        return com.luishbarros.discord_like.modules.chat.domain.model.Room.reconstitute(
                this.id,
                this.name,
                this.ownerId,
                this.memberIds,
                this.createdAt,
                this.updatedAt
        );
    }

    // Mapping from domain model
    public static RoomJpaEntity fromDomain(com.luishbarros.discord_like.modules.chat.domain.model.Room room) {
        return new RoomJpaEntity(
                room.getId(),
                room.getName(),
                room.getOwnerId(),
                room.getMemberIds(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
