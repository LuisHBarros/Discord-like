package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomEncryptionStateRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class E2EEKeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(E2EEKeyManagementService.class);

    private final RoomEncryptionStateRepository encryptionStateRepository;
    private final RoomMembershipRepository roomMembershipRepository;

    public E2EEKeyManagementService(
            RoomEncryptionStateRepository encryptionStateRepository,
            RoomMembershipRepository roomMembershipRepository) {
        this.encryptionStateRepository = encryptionStateRepository;
        this.roomMembershipRepository = roomMembershipRepository;
    }

    /**
     * Enable E2EE for a room.
     * The server acts as a blind router. The owner provides their encrypted room key.
     */
    @Transactional
    public RoomEncryptionState enableE2EE(Long roomId, Long ownerId, byte[] encryptedRoomKey) {
        try {
            RoomEncryptionState state = RoomEncryptionState.createE2EE(roomId);
            RoomEncryptionState saved = encryptionStateRepository.save(state);

            RoomMembership membership = roomMembershipRepository.findByRoomIdAndUserId(roomId, ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("User is not a member of the room"));
            
            membership.updateEncryptedRoomKey(encryptedRoomKey);
            roomMembershipRepository.save(membership);

            log.info("E2EE enabled for room {} by owner {}", roomId, ownerId);
            return saved;
        } catch (Exception e) {
            log.error("Failed to enable E2EE for room {}", roomId, e);
            throw new RuntimeException("Failed to enable E2EE", e);
        }
    }

    /**
     * Upload an encrypted room key for a specific member.
     * This allows peer-to-peer key distribution securely.
     */
    @Transactional
    public void uploadMemberKey(Long roomId, Long memberId, byte[] encryptedRoomKey) {
        RoomMembership membership = roomMembershipRepository.findByRoomIdAndUserId(roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the room"));
        
        membership.updateEncryptedRoomKey(encryptedRoomKey);
        roomMembershipRepository.save(membership);
        
        log.info("E2EE key uploaded for member {} in room {}", memberId, roomId);
    }

    /**
     * Get the encrypted room key for a member.
     */
    public byte[] getRoomKeyForMember(Long roomId, Long memberId) {
        RoomMembership membership = roomMembershipRepository.findByRoomIdAndUserId(roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the room"));
        
        return membership.getEncryptedRoomKey();
    }

    public boolean isE2EEEnabled(Long roomId) {
        return encryptionStateRepository.findByRoomId(roomId)
                .map(state -> state.mode() == RoomEncryptionState.EncryptionMode.E2EE_SIGNAL_PROTOCOL)
                .orElse(false);
    }
}
