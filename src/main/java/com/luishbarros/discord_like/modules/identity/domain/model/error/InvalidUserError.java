package com.luishbarros.discord_like.modules.identity.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidUserError extends DomainError {

    public InvalidUserError(String message) {
        super("INVALID_USER", message);
    }

    public static InvalidUserError invalidEmail(String message) {
        return new InvalidUserError("Invalid email: " + message);
    }

    public static InvalidUserError invalidUsername(String message) {
        return new InvalidUserError("Invalid username: " + message);
    }

    public static InvalidUserError invalidPassword(String message) {
        return new InvalidUserError("Invalid password: " + message);
    }
}