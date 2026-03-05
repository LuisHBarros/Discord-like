package com.luishbarros.discord_like.modules.identity.domain.model.value_object;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;

public record Username(String value) {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";

    public Username {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidUsername("Username cannot be blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw InvalidUserError.invalidUsername(
                "Username must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters"
            );
        }
        if (!value.matches(USERNAME_PATTERN)) {
            throw InvalidUserError.invalidUsername("Username can only contain letters, numbers, underscores, and hyphens");
        }
    }
}
