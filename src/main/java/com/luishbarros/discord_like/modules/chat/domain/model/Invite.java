package com.luishbarros.discord_like.modules.chat.domain.model;

import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

public class Invite extends BaseEntity {

    private static final int SECONDS_TO_ADD = 3600 * 24;

    private Long roomId;
    private Long createdByUserId;
    private InviteCode code;
    private Instant createdAt;
    private Instant expiresAt;

    protected Invite() {}

    public Invite(Long roomId, Long createdByUserId, InviteCode code, Instant createdAt) {
        this.roomId = roomId;
        this.createdByUserId = createdByUserId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusSeconds(SECONDS_TO_ADD);
    }

    public static Invite reconstitute(Long id, Long roomId, Long createdByUserId, InviteCode code, Instant createdAt, Instant expiresAt) {
        Invite invite = new Invite();
        invite.id = id;
        invite.roomId = roomId;
        invite.createdByUserId = createdByUserId;
        invite.code = code;
        invite.createdAt = createdAt;
        invite.expiresAt = expiresAt;
        return invite;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public Long getRoomId()           { return roomId; }
    public Long getCreatedByUserId()  { return createdByUserId; }
    public InviteCode getCode()       { return code; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getExpiresAt()     { return expiresAt; }
}