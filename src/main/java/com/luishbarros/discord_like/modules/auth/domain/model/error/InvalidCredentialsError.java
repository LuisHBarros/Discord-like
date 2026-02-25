package com.luishbarros.discord_like.modules.auth.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

// InvalidCredentialsError.java
public class InvalidCredentialsError extends DomainError {
    public InvalidCredentialsError(String message) {
        super("INVALID_CREDENTIALS", message);
    }
}