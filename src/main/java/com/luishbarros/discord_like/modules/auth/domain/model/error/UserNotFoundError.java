package com.luishbarros.discord_like.modules.auth.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class UserNotFoundError extends DomainError {

    public UserNotFoundError(Long id) {
        super("USER_NOT_FOUND", "User not found: " + id);
    }
    public UserNotFoundError(String email) {
        super("USER_NOT_FOUND", "User not found with: " + email);
    }
}