// shared/domain/error/RateLimitError.java
package com.luishbarros.discord_like.shared.domain.error;

public class RateLimitError extends DomainError {

    public RateLimitError(String key) {
        super("RATE_LIMITED", "Too many requests: " + key);
    }
}