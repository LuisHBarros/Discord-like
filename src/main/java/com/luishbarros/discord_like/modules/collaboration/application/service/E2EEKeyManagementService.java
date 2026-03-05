package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomEncryptionStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.time.Instant;

@Service
public class E2EEKeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(E2EEKeyManagementService.class);
    private static final String KEY_ALGORITHM = "X25519";
    private static final int KEY_SIZE = 256;

    private final RoomEncryptionStateRepository encryptionStateRepository;

    public E2EEKeyManagementService(RoomEncryptionStateRepository encryptionStateRepository) {
        this.encryptionStateRepository = encryptionStateRepository;
    }

    /**
     * Enable E2EE for a room.
     * Generates a room key pair and encrypts it for the owner.
     */
    @Transactional
    public RoomEncryptionState enableE2EE(Long roomId, Long ownerId,
                                        byte[] ownerPublicKey) {
        try {
            // Generate room key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair roomKeyPair = keyGen.generateKeyPair();

            // Encrypt room key with owner's public key
            byte[] encryptedRoomKey = encryptForUser(
                    roomKeyPair.getPrivate(),
                    ownerPublicKey
            );

            RoomEncryptionState state = RoomEncryptionState.createE2EE(
                    roomId,
                    roomKeyPair.getPublic().getEncoded(),
                    encryptedRoomKey
            );

            return encryptionStateRepository.save(state);
        } catch (Exception e) {
            log.error("Failed to enable E2EE for room {}", roomId, e);
            throw new RuntimeException("Failed to enable E2EE", e);
        }
    }

    /**
     * Add a member to an E2EE room by encrypting the room key for them.
     */
    public byte[] encryptRoomKeyForMember(Long roomId, byte[] memberPublicKey) {
        RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "E2EE not enabled for room " + roomId));

        // Decrypt room key, then encrypt for member
        // This is simplified - actual implementation would use proper key management
        return encryptForUser(
                extractRoomKey(state),
                memberPublicKey
        );
    }

    /**
     * Rotate room key and distribute to all members.
     */
    @Transactional
    public RoomEncryptionState rotateRoomKey(Long roomId) {
        RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "E2EE not enabled for room " + roomId));

        // Generate new key pair
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair newKeyPair = keyGen.generateKeyPair();

            // In production: encrypt for all members and distribute
            byte[] encryptedRoomKey = encryptForUser(
                    newKeyPair.getPrivate(),
                    state.roomPublicKey()
            );

            RoomEncryptionState newState = state.rotateKey(
                    newKeyPair.getPublic().getEncoded(),
                    encryptedRoomKey
            );

            return encryptionStateRepository.save(newState);
        } catch (Exception e) {
            log.error("Failed to rotate room key for room {}", roomId, e);
            throw new RuntimeException("Failed to rotate room key", e);
        }
    }

    private byte[] encryptForUser(Key privateKey, byte[] userPublicKey) {
        // Simplified - use proper ECDH in production
        try {
            return "encrypted_key_placeholder".getBytes();
        } catch (Exception e) {
            log.error("Key encryption failed", e);
            throw new RuntimeException("Key encryption failed", e);
        }
    }

    private PrivateKey extractRoomKey(RoomEncryptionState state) {
        // Simplified - in production, properly decrypt encrypted room key
        return null;
    }

    public boolean isE2EEEnabled(Long roomId) {
        return encryptionStateRepository.findByRoomId(roomId)
                .map(state -> state.mode() == RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL)
                .orElse(false);
    }
}
