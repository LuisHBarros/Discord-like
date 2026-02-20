package com.luishbarros.discord_like.modules.chat.domain.model.value_object;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidInviteCodeError;

import java.time.Instant;
import java.util.UUID;

public record InviteCode(
        String value,
        UUID createdByUserId
) {
    public InviteCode {
        if (value == null || value.isBlank()) {
            throw InvalidInviteCodeError.emptyCodeValue();
        }
    }
}
