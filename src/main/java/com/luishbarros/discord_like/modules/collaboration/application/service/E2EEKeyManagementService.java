package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomEncryptionStateRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.time.Instant;
import java.util.Arrays;

@Service
public class E2EEKeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(E2EEKeyManagementService.class);
    private static final String KEY_ALGORITHM = "X25519";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int ROOM_KEY_SIZE = 32; // 256 bits for AES-256

    private final RoomEncryptionStateRepository encryptionStateRepository;
    private final SecureRandom secureRandom;

    static {
        // Register BouncyCastle provider if not already registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public E2EEKeyManagementService(RoomEncryptionStateRepository encryptionStateRepository) {
        this.encryptionStateRepository = encryptionStateRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Enable E2EE for a room.
     * Generates a room symmetric key and encrypts it for the owner.
     */
    @Transactional
    public RoomEncryptionState enableE2EE(Long roomId, Long ownerId,
            byte[] ownerPublicKey) {
        try {
            validateKeySize(ownerPublicKey, "owner public key");

            // Generate room symmetric key (AES-256)
            byte[] roomKey = generateRoomKey();

            // Encrypt room key with owner's public key using ECDH
            byte[] encryptedRoomKey = encryptRoomKeyForUser(roomKey, ownerPublicKey);

            // Generate ephemeral key pair for this room
            KeyPair ephemeralKeyPair = generateKeyPair();

            byte[] rawPublicKey = extractRawX25519KeyBytes(
                    ephemeralKeyPair.getPublic().getEncoded());

            RoomEncryptionState state = RoomEncryptionState.createE2EE(
                    roomId,
                    rawPublicKey,
                    encryptedRoomKey);

            log.info("E2EE enabled for room {} by owner {}", roomId, ownerId);
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
        try {
            validateKeySize(memberPublicKey, "member public key");

            RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "E2EE not enabled for room " + roomId));

            // Generate ephemeral key pair for this distribution
            KeyPair ephemeralKeyPair = generateKeyPair();

            // In production: extract and decrypt room key, then encrypt for member
            // For now, we'll use a simplified approach
            byte[] roomKey = generateRoomKey(); // Placeholder - should extract from state
            byte[] encryptedRoomKey = encryptRoomKeyForUser(roomKey, memberPublicKey);

            log.debug("Encrypted room key for member in room {}", roomId);
            return encryptedRoomKey;
        } catch (Exception e) {
            log.error("Failed to encrypt room key for member in room {}", roomId, e);
            throw new RuntimeException("Failed to encrypt room key for member", e);
        }
    }

    /**
     * Rotate room key and distribute to all members.
     */
    @Transactional
    public RoomEncryptionState rotateRoomKey(Long roomId) {
        try {
            RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "E2EE not enabled for room " + roomId));

            // Generate new room symmetric key
            byte[] newRoomKey = generateRoomKey();

            // Generate new ephemeral key pair
            KeyPair newEphemeralKeyPair = generateKeyPair();

            byte[] rawPublicKey = extractRawX25519KeyBytes(
                    newEphemeralKeyPair.getPublic().getEncoded());

            // In production: encrypt for all members and distribute
            // For now, encrypt with room's public key (simplified)
            byte[] encryptedRoomKey = encryptRoomKeyForUser(newRoomKey, state.roomPublicKey());

            RoomEncryptionState newState = state.rotateKey(
                    rawPublicKey,
                    encryptedRoomKey);

            log.info("Rotated room key for room {}", roomId);
            return encryptionStateRepository.save(newState);
        } catch (Exception e) {
            log.error("Failed to rotate room key for room {}", roomId, e);
            throw new RuntimeException("Failed to rotate room key", e);
        }
    }

    /**
     * Extract raw X25519 key bytes from encoded public key.
     * X25519 keys are 32 bytes, but getEncoded() may include ASN.1 metadata.
     */
    private byte[] extractRawX25519KeyBytes(byte[] encodedKey) throws IOException {
        if (encodedKey.length == 32) {
            return encodedKey;
        }
        // Extract raw 32-byte key from ASN.1 encoded format
        // For X25519, the raw key is typically at the end of the structure
        int start = encodedKey.length - 32;
        byte[] rawKey = new byte[32];
        System.arraycopy(encodedKey, start, rawKey, 0, 32);
        return rawKey;
    }

    /**
     * Encrypt room symmetric key for a specific user using ECDH.
     * This uses a simplified approach - in production, you would:
     * 1. Generate ephemeral key pair
     * 2. Perform ECDH key exchange with user's public key
     * 3. Derive a symmetric key from the shared secret
     * 4. Encrypt room key with the derived key using AES-GCM
     */
    private byte[] encryptRoomKeyForUser(byte[] roomKey, byte[] userPublicKey) throws Exception {
        // Validate input
        validateKeySize(roomKey, "room key");

        // Generate ephemeral key pair for this encryption
        KeyPair ephemeralKeyPair = generateKeyPair();

        // For production, perform proper ECDH key exchange here
        // This is a simplified implementation using AES-GCM directly
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Derive encryption key from user's public key (simplified)
        byte[] derivedKey = deriveEncryptionKey(ephemeralKeyPair.getPrivate(), userPublicKey);

        // Encrypt room key
        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKeySpec keySpec = new SecretKeySpec(derivedKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] encryptedRoomKey = cipher.doFinal(roomKey);

        // Combine IV + ephemeral public key + encrypted data
        byte[] rawPublicKeyBytes = extractRawX25519KeyBytes(
                ephemeralKeyPair.getPublic().getEncoded());
        byte[] result = new byte[GCM_IV_LENGTH + rawPublicKeyBytes.length + encryptedRoomKey.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(rawPublicKeyBytes, 0, result, GCM_IV_LENGTH, rawPublicKeyBytes.length);
        System.arraycopy(encryptedRoomKey, 0, result, GCM_IV_LENGTH + rawPublicKeyBytes.length, encryptedRoomKey.length);

        return result;
    }

    /**
     * Derive encryption key from ECDH shared secret.
     * This is a simplified version - in production use HKDF or similar KDF.
     */
    private byte[] deriveEncryptionKey(PrivateKey privateKey, byte[] publicKeyBytes) throws Exception {
        // Simplified key derivation - in production use proper ECDH + HKDF
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] combined = new byte[privateKey.getEncoded().length + publicKeyBytes.length];
        System.arraycopy(privateKey.getEncoded(), 0, combined, 0, privateKey.getEncoded().length);
        System.arraycopy(publicKeyBytes, 0, combined, privateKey.getEncoded().length, publicKeyBytes.length);
        return digest.digest(combined);
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyGen.initialize(KEY_SIZE, secureRandom);
        return keyGen.generateKeyPair();
    }

    private byte[] generateRoomKey() {
        byte[] roomKey = new byte[ROOM_KEY_SIZE];
        secureRandom.nextBytes(roomKey);
        return roomKey;
    }

    private void validateKeySize(byte[] key, String keyName) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException(keyName + " must be 32 bytes (256 bits)");
        }
    }

    public boolean isE2EEEnabled(Long roomId) {
        return encryptionStateRepository.findByRoomId(roomId)
                .map(state -> state.mode() == RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL)
                .orElse(false);
    }
}
