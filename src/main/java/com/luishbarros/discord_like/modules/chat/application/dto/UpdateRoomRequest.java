package com.luishbarros.discord_like.modules.chat.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
        @Size(min = 1, max = 100, message = "Room name must be between 1 and 100 characters")
        String name
) {
}
