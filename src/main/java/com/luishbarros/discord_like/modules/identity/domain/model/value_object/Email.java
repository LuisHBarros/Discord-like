package com.luishbarros.discord_like.modules.identity.domain.model.value_object;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;

public record Email(String value) {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidEmail("Email cannot be blank");
        }
        if (!value.matches(EMAIL_PATTERN)) {
            throw InvalidUserError.invalidEmail("Invalid email format");
        }
    }
}
