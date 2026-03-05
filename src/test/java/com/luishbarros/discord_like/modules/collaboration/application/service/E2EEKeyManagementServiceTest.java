package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.*;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class E2EEKeyManagementServiceTest {

    @Autowired
    private E2EEKeyManagementService keyManagementService;

    @Test
    void enableE2EE_shouldCreateEncryptionState() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] publicKey = generateValidPublicKey();

        RoomEncryptionState state = keyManagementService.enableE2EE(
                roomId,
                ownerId,
                publicKey
        );

        assertThat(state.roomId()).isEqualTo(roomId);
        assertThat(state.mode()).isEqualTo(
                RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL);
        assertThat(state.roomPublicKey()).isNotNull();
        assertThat(state.roomPublicKey().length).isEqualTo(32); // X25519 key size
    }

    @Test
    void enableE2EE_withInvalidKeySize_shouldThrowException() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] invalidPublicKey = new byte[16]; // Wrong size

        assertThatThrownBy(() -> keyManagementService.enableE2EE(roomId, ownerId, invalidPublicKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be 32 bytes");
    }

    @Test
    void isE2EEEnabled_shouldReturnCorrectStatus() {
        Long roomId = 1L;

        boolean isEnabled = keyManagementService.isE2EEEnabled(roomId);

        assertThat(isEnabled).isFalse(); // Default
    }

    @Test
    void rotateRoomKey_shouldGenerateNewKey() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] publicKey = generateValidPublicKey();

        // First, enable E2EE
        RoomEncryptionState initialState = keyManagementService.enableE2EE(roomId, ownerId, publicKey);

        // Rotate key
        RoomEncryptionState rotatedState = keyManagementService.rotateRoomKey(roomId);

        assertThat(rotatedState.roomId()).isEqualTo(roomId);
        assertThat(rotatedState.mode()).isEqualTo(
                RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL);
        assertThat(rotatedState.roomPublicKey()).isNotNull();
        // The rotated key should be different from the initial key
        assertThat(rotatedState.roomPublicKey()).isNotEqualTo(initialState.roomPublicKey());
    }

    @Test
    void encryptRoomKeyForMember_shouldGenerateValidEncryptedKey() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] ownerPublicKey = generateValidPublicKey();
        byte[] memberPublicKey = generateValidPublicKey();

        // Enable E2EE
        keyManagementService.enableE2EE(roomId, ownerId, ownerPublicKey);

        // Encrypt for member
        byte[] encryptedKey = keyManagementService.encryptRoomKeyForMember(roomId, memberPublicKey);

        assertThat(encryptedKey).isNotNull();
        // Format: IV (12) + ephemeral public key (32) + encrypted data
        assertThat(encryptedKey.length).isGreaterThan(44); // At least 12 + 32 bytes
    }

    @Test
    void encryptRoomKeyForMember_withInvalidKeySize_shouldThrowException() {
        Long roomId = 1L;
        byte[] invalidPublicKey = new byte[16]; // Wrong size

        assertThatThrownBy(() -> keyManagementService.encryptRoomKeyForMember(roomId, invalidPublicKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be 32 bytes");
    }

    /**
     * Generate a valid 32-byte public key for testing.
     * In a real scenario, this would be generated using KeyPairGenerator.
     */
    private byte[] generateValidPublicKey() {
        byte[] publicKey = new byte[32];
        new SecureRandom().nextBytes(publicKey);
        return publicKey;
    }
}
