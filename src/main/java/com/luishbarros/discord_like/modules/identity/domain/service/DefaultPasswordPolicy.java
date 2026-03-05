package com.luishbarros.discord_like.modules.identity.domain.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class DefaultPasswordPolicy implements PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int EXPIRY_DAYS = 90;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    @Override
    public boolean isPasswordExpired(Instant lastChangedAt) {
        return lastChangedAt == null ||
               Duration.between(lastChangedAt, Instant.now()).toDays() > EXPIRY_DAYS;
    }

    @Override
    public boolean isPasswordValid(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            return false;
        }
        return UPPERCASE.matcher(plainPassword).find() &&
               LOWERCASE.matcher(plainPassword).find() &&
               DIGIT.matcher(plainPassword).find() &&
               SPECIAL.matcher(plainPassword).find();
    }

    @Override
    public int getMinPasswordLength() {
        return MIN_LENGTH;
    }

    @Override
    public int getPasswordExpiryDays() {
        return EXPIRY_DAYS;
    }
}
