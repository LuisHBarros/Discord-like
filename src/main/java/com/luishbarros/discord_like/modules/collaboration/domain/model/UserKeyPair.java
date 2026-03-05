package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

/**
 * Stores a user's E2EE key pair.
 */
public record UserKeyPair(
        Long id,
        Long userId,
        byte[] publicKey,
        byte[] privateKey, // Encrypted at rest with user's password
        Instant createdAt,
        Instant lastUsedAt
) {
    public static UserKeyPair create(Long userId, byte[] publicKey, byte[] encryptedPrivateKey) {
        return new UserKeyPair(
                null,
                userId,
                publicKey,
                encryptedPrivateKey,
                Instant.now(),
                Instant.now()
        );
    }
}
