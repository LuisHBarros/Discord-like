package com.luishbarros.discord_like.modules.collaboration.domain.model.value_object;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidInviteCodeError;

public record InviteCode(String value) {
    public InviteCode {
        if (value == null || value.isBlank()) {
            throw InvalidInviteCodeError.emptyCodeValue();
        }
    }
}
