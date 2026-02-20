package com.luishbarros.discord_like.modules.chat.domain.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class InvalidInviteCodeError extends DomainError {

    public InvalidInviteCodeError(String message) {
        super("INVALID_INVITE_CODE", message);
    }

    public static InvalidInviteCodeError emptyCodeValue() {
        return new InvalidInviteCodeError("Invite code value cannot be empty");
    }
}