package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

public class RoomMembership extends BaseEntity {

    private final Long roomId;
    private final Long userId;
    private final Instant joinedAt;
    private byte[] encryptedRoomKey;

    public RoomMembership(Long id, Long roomId, Long userId, Instant joinedAt, byte[] encryptedRoomKey) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.encryptedRoomKey = encryptedRoomKey;
    }

    public static RoomMembership create(Long roomId, Long userId, Instant joinedAt) {
        return new RoomMembership(null, roomId, userId, joinedAt, null);
    }

    public static RoomMembership reconstitute(Long id, Long roomId, Long userId, Instant joinedAt, byte[] encryptedRoomKey) {
        return new RoomMembership(id, roomId, userId, joinedAt, encryptedRoomKey);
    }

    public void updateEncryptedRoomKey(byte[] encryptedRoomKey) {
        this.encryptedRoomKey = encryptedRoomKey;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public byte[] getEncryptedRoomKey() {
        return encryptedRoomKey;
    }
}
