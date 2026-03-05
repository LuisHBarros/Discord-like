package com.luishbarros.discord_like.modules.collaboration.domain.model.value_object;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;

public record MessageContent(String ciphertext) {
    private static final int MIN_CIPHERTEXT_LENGTH = 12; // IV + at least some data

    public MessageContent {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
        if (ciphertext.length() < MIN_CIPHERTEXT_LENGTH) {
            throw InvalidMessageError.invalidFormat("Ciphertext too short");
        }
    }
}
