package com.luishbarros.discord_like.modules.chat.application.dto;

import com.luishbarros.discord_like.modules.chat.domain.model.Invite;

import java.time.Instant;

public record InviteResponse(
        Long id,
        String code,
        Instant expiresAt
) {
    public static InviteResponse fromInvite(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getCode().value(),
                invite.getExpiresAt()
        );
    }
}