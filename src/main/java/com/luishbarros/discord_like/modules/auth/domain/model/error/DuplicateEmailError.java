package com.luishbarros.discord_like.modules.auth.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class DuplicateEmailError extends DomainError {
    public DuplicateEmailError(String email) {
        super("DUPLICATE_EMAIL", "Email already in use: " + email);
    }
}