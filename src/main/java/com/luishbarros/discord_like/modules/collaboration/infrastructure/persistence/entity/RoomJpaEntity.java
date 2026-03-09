package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.RoomName;
import jakarta.persistence.*;


import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
public class RoomJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomJpaEntity() {}

    public RoomJpaEntity(Long id, String name, Long ownerId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Mapping to domain model
    public com.luishbarros.discord_like.modules.collaboration.domain.model.Room toDomain() {
        return com.luishbarros.discord_like.modules.collaboration.domain.model.Room.reconstitute(
                this.id,
                new RoomName(this.name),
                this.ownerId,
                this.createdAt,
                this.updatedAt
        );
    }

    // Mapping from domain model
    public static RoomJpaEntity fromDomain(com.luishbarros.discord_like.modules.collaboration.domain.model.Room room) {
        return new RoomJpaEntity(
                room.getId(),
                room.getName().value(),
                room.getOwnerId(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
