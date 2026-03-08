package com.luishbarros.discord_like.modules.collaboration.infrastructure.http;

import com.luishbarros.discord_like.modules.collaboration.application.service.E2EEKeyManagementService;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/e2ee")
public class E2EEController {

        private final E2EEKeyManagementService keyManagementService;

        public E2EEController(E2EEKeyManagementService keyManagementService) {
                this.keyManagementService = keyManagementService;
        }

        /**
         * Enable E2EE for a room (room owner only)
         */
        @PostMapping("/rooms/{roomId}/enable")
        public ResponseEntity<RoomEncryptionResponse> enableE2EE(
                        @PathVariable Long roomId,
                        @RequestHeader("X-User-Id") Long userId,
                        @RequestBody EnableE2ERequest request) {
                // Validate ownership (use RoomService)
                var state = keyManagementService.enableE2EE(
                                roomId,
                                userId,
                                Base64.getDecoder().decode(request.publicKey()));

                return ResponseEntity.ok(RoomEncryptionResponse.fromState(state));
        }

        /**
         * Get room encryption key for a member
         */
        @GetMapping("/rooms/{roomId}/key")
        public ResponseEntity<EncryptedKeyResponse> getRoomKey(
                        @PathVariable Long roomId,
                        @RequestHeader("X-User-Id") Long userId,
                        @RequestBody GetRoomKeyRequest request) {
                // Validate membership first

                byte[] encryptedKey = keyManagementService.encryptRoomKeyForMember(
                                roomId,
                                Base64.getDecoder().decode(request.publicKey()));

                return ResponseEntity.ok(new EncryptedKeyResponse(
                                Base64.getEncoder().encodeToString(encryptedKey)));
        }

        /**
         * Rotate room key (room owner only)
         */
        @PostMapping("/rooms/{roomId}/rotate-key")
        public ResponseEntity<RoomEncryptionResponse> rotateKey(
                        @PathVariable Long roomId,
                        @RequestHeader("X-User-Id") Long userId) {
                // Validate ownership

                var state = keyManagementService.rotateRoomKey(roomId);
                return ResponseEntity.ok(RoomEncryptionResponse.fromState(state));
        }
}

record EnableE2ERequest(String publicKey) {
}

record GetRoomKeyRequest(String publicKey) {
}

record RoomEncryptionResponse(
                Long roomId,
                String mode,
                String publicKey,
                String encryptedKey) {
        static RoomEncryptionResponse fromState(RoomEncryptionState state) {
                return new RoomEncryptionResponse(
                                state.roomId(),
                                state.mode().name(),
                                Base64.getEncoder().encodeToString(state.roomPublicKey()),
                                state.encryptedRoomKey() != null
                                                ? Base64.getEncoder().encodeToString(state.encryptedRoomKey())
                                                : null);
        }
}

record EncryptedKeyResponse(String encryptedKey) {
}
