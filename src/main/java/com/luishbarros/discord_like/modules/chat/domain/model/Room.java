package com.luishbarros.discord_like.modules.chat.domain.model;

import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Room {

    private UUID id;
    private String name;
    private UUID ownerId;
    private final Set<UUID> memberIds = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    protected Room() {
    }

    public Room(String name, UUID ownerId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds.add(ownerId);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Room reconstitute(UUID id, String name, UUID ownerId, Set<UUID> memberIds, Instant createdAt, Instant updatedAt) {
        Room room = new Room();
        room.id = id;
        room.name = name;
        room.ownerId = ownerId;
        room.memberIds.addAll(memberIds);
        room.createdAt = createdAt;
        room.updatedAt = updatedAt;
        return room;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name, Instant updatedAt) {
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<UUID> getMemberIds() {
        return Set.copyOf(memberIds);
    }

    public void addMember(UUID userId, Instant updatedAt) {
        if (userId == null) {
            throw new InvalidRoomError("User ID cannot be null");
        }
        this.memberIds.add(userId);
        this.updatedAt = updatedAt;
    }

    public void removeMember(UUID userId, Instant updatedAt) {
        if (this.memberIds.size() <= 1) {
            throw new InvalidRoomError("Cannot remove last member");
        }
        if (this.ownerId.equals(userId)) {
            throw new InvalidRoomError("Cannot remove room owner");
        }
        this.memberIds.remove(userId);
        this.updatedAt = updatedAt;
    }

    public boolean isMember(UUID userId) {
        return this.memberIds.contains(userId);
    }

    public boolean isOwner(UUID userId) {
        return this.ownerId.equals(userId);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
