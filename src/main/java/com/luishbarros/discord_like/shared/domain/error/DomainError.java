package com.luishbarros.discord_like.shared.domain.error;

public abstract class DomainError extends RuntimeException {

    private final String code;

    protected DomainError(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
