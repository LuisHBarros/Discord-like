package com.luishbarros.discord_like.modules.collaboration.domain.model.value_object;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;

public record MessageContent(String ciphertext) {

    public MessageContent {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
    }
}
