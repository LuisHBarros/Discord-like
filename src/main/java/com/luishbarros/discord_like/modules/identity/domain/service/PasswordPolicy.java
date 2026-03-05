package com.luishbarros.discord_like.modules.identity.domain.service;

import java.time.Instant;

public interface PasswordPolicy {
    boolean isPasswordExpired(Instant lastChangedAt);
    boolean isPasswordValid(String plainPassword);
    int getMinPasswordLength();
    int getPasswordExpiryDays();
}
