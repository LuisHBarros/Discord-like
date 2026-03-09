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
                
                var state = keyManagementService.enableE2EE(
                                roomId,
                                userId,
                                Base64.getDecoder().decode(request.encryptedRoomKey()));

                return ResponseEntity.ok(RoomEncryptionResponse.fromState(state));
        }

        /**
         * Upload encrypted room key for a specific member
         */
        @PutMapping("/rooms/{roomId}/members/{memberId}/key")
        public ResponseEntity<Void> uploadMemberKey(
                        @PathVariable Long roomId,
                        @PathVariable Long memberId,
                        @RequestHeader("X-User-Id") Long uploaderId,
                        @RequestBody UploadKeyRequest request) {

                keyManagementService.uploadMemberKey(
                                roomId,
                                memberId,
                                Base64.getDecoder().decode(request.encryptedRoomKey()));

                return ResponseEntity.ok().build();
        }

        /**
         * Get room encryption key for the requesting member
         */
        @GetMapping("/rooms/{roomId}/key")
        public ResponseEntity<EncryptedKeyResponse> getRoomKey(
                        @PathVariable Long roomId,
                        @RequestHeader("X-User-Id") Long userId) {

                byte[] encryptedKey = keyManagementService.getRoomKeyForMember(
                                roomId,
                                userId);

                return ResponseEntity.ok(new EncryptedKeyResponse(
                                encryptedKey != null ? Base64.getEncoder().encodeToString(encryptedKey) : null));
        }
}

record EnableE2ERequest(String encryptedRoomKey) {
}

record UploadKeyRequest(String encryptedRoomKey) {
}

record RoomEncryptionResponse(
                Long roomId,
                String mode) {
        static RoomEncryptionResponse fromState(RoomEncryptionState state) {
                return new RoomEncryptionResponse(
                                state.roomId(),
                                state.mode().name());
        }
}

record EncryptedKeyResponse(String encryptedKey) {
}
