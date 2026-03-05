package com.luishbarros.discord_like.modules.collaboration.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidMessageError extends DomainError {

    public InvalidMessageError(String message) {
        super("INVALID_MESSAGE", message);
    }

    public static InvalidMessageError emptyContent() {
        return new InvalidMessageError("Message content cannot be empty");
    }

    public static InvalidMessageError notFound(String messageId) {
        return new InvalidMessageError("Message not found: " + messageId);
    }

    public static InvalidMessageError invalidFormat(String message) {
        return new InvalidMessageError("Invalid message format: " + message);
    }

    public static InvalidMessageError messageLimitExceeded(String message) {
        return new InvalidMessageError(message);
    }
}