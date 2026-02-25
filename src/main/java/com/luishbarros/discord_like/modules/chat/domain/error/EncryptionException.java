package com.luishbarros.discord_like.modules.chat.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class EncryptionException extends DomainError {

    public EncryptionException(String message) {
        super("ENCRYPTION_ERROR", message);
    }
}