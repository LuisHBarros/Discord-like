package com.luishbarros.discord_like.modules.collaboration.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class ForbiddenError extends DomainError {

    public ForbiddenError(String message) {
        super("FORBIDDEN", message);
    }
}
