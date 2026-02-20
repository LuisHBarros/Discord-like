package com.luishbarros.discord_like.modules.chat.domain.model;


import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;

import java.time.Instant;
import java.util.UUID;

public class Invite {
    private static final int SECONDS_TO_ADD = 3600 * 24; // A day
    private final UUID id;
    private final UUID roomId;
    private final UUID createdByUserId;
    private final InviteCode code;
    private final Instant createdAt;
    private final Instant expiresAt;

    public Invite(UUID roomId, UUID createdByUserId, InviteCode code, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.createdByUserId = createdByUserId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusSeconds(SECONDS_TO_ADD);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public UUID getRoomId() { return roomId; }
    public InviteCode getCode() { return code; }

    public UUID getId() {
        return this.id;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
