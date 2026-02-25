package com.luishbarros.discord_like.modules.chat.domain.model;

import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class Room extends BaseEntity {

    private String name;
    private Long ownerId;
    private final Set<Long> memberIds = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    protected Room() {}

    public Room(String name, Long ownerId, Instant createdAt) {
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds.add(ownerId);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Room reconstitute(Long id, String name, Long ownerId, Set<Long> memberIds, Instant createdAt, Instant updatedAt) {
        Room room = new Room();
        room.id = id;
        room.name = name;
        room.ownerId = ownerId;
        room.memberIds.addAll(memberIds);
        room.createdAt = createdAt;
        room.updatedAt = updatedAt;
        return room;
    }

    public void setName(String name, Instant updatedAt) {
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public void addMember(Long userId, Instant updatedAt) {
        if (userId == null) {
            throw new InvalidRoomError("User ID cannot be null");
        }
        this.memberIds.add(userId);
        this.updatedAt = updatedAt;
    }

    public void removeMember(Long userId, Instant updatedAt) {
        if (this.memberIds.size() <= 1) {
            throw new InvalidRoomError("Cannot remove last member");
        }
        if (this.ownerId.equals(userId)) {
            throw new InvalidRoomError("Cannot remove room owner");
        }
        this.memberIds.remove(userId);
        this.updatedAt = updatedAt;
    }

    public boolean isMember(Long userId) {
        return this.memberIds.contains(userId);
    }

    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    public String getName()         { return name; }
    public Long getOwnerId()        { return ownerId; }
    public Set<Long> getMemberIds() { return Set.copyOf(memberIds); }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }
}