package com.luishbarros.discord_like.modules.identity.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

// InvalidTokenError.java
public class InvalidTokenError extends DomainError {
    public InvalidTokenError(String message) {
        super("INVALID_TOKEN", message);
    }
}