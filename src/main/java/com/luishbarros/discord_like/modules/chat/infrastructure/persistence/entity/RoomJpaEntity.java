package com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity;

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

    @ElementCollection
    @CollectionTable(
            name = "room_members",
            joinColumns = @JoinColumn(name = "room_id"),
            indexes = @Index(name = "idx_room_members_user_room", columnList = "user_id, room_id")
    )
    @Column(name = "user_id", nullable = false)
    private Set<Long> memberIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomJpaEntity() {}

    public RoomJpaEntity(Long id, String name, Long ownerId, Set<Long> memberIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds = new HashSet<>(memberIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public Set<Long> getMemberIds() { return Set.copyOf(memberIds); }
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
