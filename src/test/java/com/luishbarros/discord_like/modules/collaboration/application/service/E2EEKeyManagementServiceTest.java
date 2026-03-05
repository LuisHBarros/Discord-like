package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class E2EEKeyManagementServiceTest {

    @Autowired
    private E2EEKeyManagementService keyManagementService;

    @Test
    void enableE2EE_shouldCreateEncryptionState() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] publicKey = "test_public_key".getBytes();

        RoomEncryptionState state = keyManagementService.enableE2EE(
                roomId,
                ownerId,
                publicKey
        );

        assertThat(state.roomId()).isEqualTo(roomId);
        assertThat(state.mode()).isEqualTo(
                RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL);
        assertThat(state.roomPublicKey()).isNotNull();
    }

    @Test
    void isE2EEEnabled_shouldReturnCorrectStatus() {
        Long roomId = 1L;

        boolean isEnabled = keyManagementService.isE2EEEnabled(roomId);

        assertThat(isEnabled).isFalse(); // Default
    }
}
