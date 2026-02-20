package com.luishbarros.discord_like.modules.chat.domain.event;

import com.luishbarros.discord_like.modules.chat.domain.model.Invite;

import java.time.Instant;
import java.util.UUID;

public record InviteEvents(
        UUID inviteId,
        UUID roomId,
        UUID createdByUserId,
        String inviteCode,
        Instant createdAt,
        Instant expiresAt,
        UUID acceptedByUserId,
        EventType type
) {

    public enum EventType {
        CREATED,
        ACCEPTED,
        REVOKED
    }

    public static InviteEvents created(Invite invite) {
        return new InviteEvents(
                invite.getId(),
                invite.getRoomId(),
                invite.getCreatedByUserId(),
                invite.getCode().value(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                null,
                EventType.CREATED
        );
    }

    public static InviteEvents accepted(Invite invite, UUID userId) {
        return new InviteEvents(
                invite.getId(),
                invite.getRoomId(),
                invite.getCreatedByUserId(),
                invite.getCode().value(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                userId,
                EventType.ACCEPTED
        );
    }

    public static InviteEvents revoked(Invite invite) {
        return new InviteEvents(
                invite.getId(),
                invite.getRoomId(),
                invite.getCreatedByUserId(),
                invite.getCode().value(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                null,
                EventType.REVOKED
        );
    }
}