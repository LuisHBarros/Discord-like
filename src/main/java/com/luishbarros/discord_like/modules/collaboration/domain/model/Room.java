package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.RoomName;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class Room extends BaseEntity {

    private RoomName name;
    private Long ownerId;
    private Instant createdAt;
    private Instant updatedAt;

    protected Room() {}

    public Room(RoomName name, Long ownerId, Instant createdAt) {
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Room reconstitute(Long id, RoomName name, Long ownerId, Instant createdAt, Instant updatedAt) {
        Room room = new Room();
        room.id = id;
        room.name = name;
        room.ownerId = ownerId;
        room.createdAt = createdAt;
        room.updatedAt = updatedAt;
        return room;
    }

    public void setName(RoomName name, Instant updatedAt) {
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    public RoomName getName()       { return name; }
    public Long getOwnerId()        { return ownerId; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }
}
