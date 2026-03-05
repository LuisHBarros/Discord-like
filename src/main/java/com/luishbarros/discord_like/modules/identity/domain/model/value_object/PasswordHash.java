package com.luishbarros.discord_like.modules.identity.domain.model.value_object;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidPassword("Password hash cannot be blank");
        }
    }
}
