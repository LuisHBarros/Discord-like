package com.luishbarros.discord_like.modules.auth.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidUserError extends DomainError {

    public InvalidUserError(String message) {
        super("INVALID_USER", message);
    }
}