package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

/**
 * Tracks encryption state for a room.
 */
public class RoomEncryptionState extends BaseEntity {

    private final Long roomId;
    private final EncryptionMode mode;
    private final Instant keyRotatedAt;
    private final Instant createdAt;

    public enum EncryptionMode {
        LEGACY_AES_GCM,
        E2EE_SIGNAL_PROTOCOL
    }

    private RoomEncryptionState(Long id, Long roomId, EncryptionMode mode,
            Instant keyRotatedAt, Instant createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.mode = mode;
        this.keyRotatedAt = keyRotatedAt;
        this.createdAt = createdAt;
    }

    public static RoomEncryptionState createLegacy(Long roomId) {
        return new RoomEncryptionState(
                null,
                roomId,
                EncryptionMode.LEGACY_AES_GCM,
                Instant.now(),
                Instant.now());
    }

    public static RoomEncryptionState createE2EE(Long roomId) {
        return new RoomEncryptionState(
                null,
                roomId,
                EncryptionMode.E2EE_SIGNAL_PROTOCOL,
                Instant.now(),
                Instant.now());
    }

    public RoomEncryptionState rotateKey() {
        return new RoomEncryptionState(
                this.id,
                this.roomId,
                this.mode,
                Instant.now(),
                this.createdAt);
    }

    public static RoomEncryptionState reconstitute(Long id, Long roomId, EncryptionMode mode,
            Instant keyRotatedAt, Instant createdAt) {
        return new RoomEncryptionState(
                id,
                roomId,
                mode,
                keyRotatedAt,
                createdAt);
    }

    // Getters...
    public Long roomId() {
        return roomId;
    }

    public EncryptionMode mode() {
        return mode;
    }

    public Instant keyRotatedAt() {
        return keyRotatedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
