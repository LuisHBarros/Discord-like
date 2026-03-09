package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.BaseIntegrationTest;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class E2EEKeyManagementServiceTest extends BaseIntegrationTest {

    @Autowired
    private E2EEKeyManagementService keyManagementService;

    @Autowired
    private RoomMembershipRepository membershipRepository;

    @AfterEach
    public void setupData() {
        // Clear between tests
    }

    @Test
    void enableE2EE_shouldCreateEncryptionStateAndSaveKeyForOwner() {
        Long roomId = 1L;
        Long ownerId = 1L;
        byte[] encryptedKey = generateValidKey();

        // Must be a member first
        membershipRepository.save(RoomMembership.create(roomId, ownerId, Instant.now()));

        RoomEncryptionState state = keyManagementService.enableE2EE(
                roomId,
                ownerId,
                encryptedKey);

        assertThat(state.roomId()).isEqualTo(roomId);
        assertThat(state.mode()).isEqualTo(
                RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL);

        // Verify key was saved to membership
        byte[] retrievedKey = keyManagementService.getRoomKeyForMember(roomId, ownerId);
        assertThat(retrievedKey).isEqualTo(encryptedKey);
    }

    @Test
    void isE2EEEnabled_shouldReturnCorrectStatus() {
        Long roomId = 1L;
        boolean isEnabled = keyManagementService.isE2EEEnabled(roomId);
        assertThat(isEnabled).isFalse();
    }

    @Test
    void uploadMemberKey_shouldSaveKeyForMember() {
        Long roomId = 1L;
        Long memberId = 2L;
        byte[] encryptedKey = generateValidKey();

        // Must be a member first
        membershipRepository.save(RoomMembership.create(roomId, memberId, Instant.now()));

        keyManagementService.uploadMemberKey(roomId, memberId, encryptedKey);

        byte[] retrievedKey = keyManagementService.getRoomKeyForMember(roomId, memberId);
        assertThat(retrievedKey).isEqualTo(encryptedKey);
    }

    private byte[] generateValidKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
