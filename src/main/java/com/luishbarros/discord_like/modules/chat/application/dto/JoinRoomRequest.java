package com.luishbarros.discord_like.modules.chat.application.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode
) {
}
