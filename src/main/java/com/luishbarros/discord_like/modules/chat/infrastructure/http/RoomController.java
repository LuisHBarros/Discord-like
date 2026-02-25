package com.luishbarros.discord_like.modules.chat.infrastructure.http;

import com.luishbarros.discord_like.modules.chat.application.dto.*;
import com.luishbarros.discord_like.modules.chat.application.service.InviteService;
import com.luishbarros.discord_like.modules.chat.application.service.RoomService;
import com.luishbarros.discord_like.modules.chat.domain.model.Invite;
import com.luishbarros.discord_like.modules.auth.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final InviteService inviteService;

    public RoomController(RoomService roomService, InviteService inviteService) {
        this.roomService = roomService;
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        RoomResponse response = RoomResponse.fromRoom(
                roomService.createRoom(request.name(), principal.getUserId(), Instant.now())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<RoomResponse> response = roomService.findByMemberId(principal.getUserId())
                .stream()
                .map(RoomResponse::fromRoom)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        RoomResponse response = RoomResponse.fromRoom(
                roomService.findById(id, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomResponse response = RoomResponse.fromRoom(
                roomService.updateRoomName(id, principal.getUserId(), request.name(), Instant.now())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        roomService.deleteRoom(id, principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    public ResponseEntity<Void> joinRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody JoinRoomRequest request
    ) {
        inviteService.acceptInvite(request.inviteCode(), principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        roomService.leaveRoom(id, principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invite/regenerate")
    public ResponseEntity<InviteResponse> regenerateInvite(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        Invite invite = inviteService.generateInvite(id, principal.getUserId(), Instant.now());
        return ResponseEntity.ok(InviteResponse.fromInvite(invite));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Set<Long>> getMembers(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(roomService.getMembers(id, principal.getUserId()));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @PathVariable Long memberId
    ) {
        roomService.removeMember(id, principal.getUserId(), memberId, Instant.now());
        return ResponseEntity.noContent().build();
    }
}