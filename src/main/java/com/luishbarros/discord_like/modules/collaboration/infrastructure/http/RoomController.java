package com.luishbarros.discord_like.modules.collaboration.infrastructure.http;

import com.luishbarros.discord_like.modules.collaboration.application.dto.*;
import com.luishbarros.discord_like.modules.collaboration.application.service.InviteService;
import com.luishbarros.discord_like.modules.collaboration.application.service.RoomService;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import com.luishbarros.discord_like.modules.identity.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/rooms")
@Tag(name = "Rooms", description = "Room management and membership operations")
public class RoomController {

    private final RoomService roomService;
    private final InviteService inviteService;

    public RoomController(RoomService roomService, InviteService inviteService) {
        this.roomService = roomService;
        this.inviteService = inviteService;
    }

    @Operation(
            summary = "Create a new room",
            description = "Creates a new chat room with the specified name. The creating user automatically becomes the room owner and member."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Room created successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid room name"
            )
    })
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(
                    description = "Room creation request with room name",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_room",
                                            value = "{\"name\":\"General Chat\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody CreateRoomRequest request
    ) {
        Room room = roomService.createRoom(request.name(), principal.getUserId(), Instant.now());
        Set<Long> memberIds = roomService.getMembers(room.getId(), principal.getUserId());
        RoomResponse response = RoomResponse.fromRoom(room, memberIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List user's rooms",
            description = "Returns a list of all rooms the authenticated user is a member of."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of rooms retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Long.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<RoomResponse> response = roomService.findByMemberId(principal.getUserId())
                .stream()
                .map(room -> {
                    Set<Long> memberIds = roomService.getMembers(room.getId(), principal.getUserId());
                    return RoomResponse.fromRoom(room, memberIds);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get room details",
            description = "Returns detailed information about a specific room. User must be a member of the room."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not a member of the room"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id
    ) {
        Room room = roomService.findById(id, principal.getUserId());
        Set<Long> memberIds = roomService.getMembers(room.getId(), principal.getUserId());
        RoomResponse response = RoomResponse.fromRoom(room, memberIds);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update room name",
            description = "Renames the specified room. Only the room owner can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room name updated successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid room name"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not the room owner"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @PatchMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id,
            @Parameter(
                    description = "New room name",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_rename",
                                            value = "{\"name\":\"New Room Name\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        Room room = roomService.updateRoomName(id, principal.getUserId(), request.name(), Instant.now());
        Set<Long> memberIds = roomService.getMembers(room.getId(), principal.getUserId());
        RoomResponse response = RoomResponse.fromRoom(room, memberIds);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a room",
            description = "Permanently deletes the specified room and all its messages. Only the room owner can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Room deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not the room owner"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id
    ) {
        roomService.deleteRoom(id, principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Join a room with invite code",
            description = "Joins a room using a valid invite code. The invite code is verified for validity and expiration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Joined room successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired invite code"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invite code not found"
            )
    })
    @PostMapping("/join")
    public ResponseEntity<Void> joinRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(
                    description = "Invite code",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_invite",
                                            value = "{\"inviteCode\":\"AB12CD34\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody JoinRoomRequest request
    ) {
        inviteService.acceptInvite(request.inviteCode(), principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Leave a room",
            description = "Removes the authenticated user from a room. The room owner cannot leave their own room."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Left room successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Room owner cannot leave their own room"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not a member of the room"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id
    ) {
        roomService.leaveRoom(id, principal.getUserId(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Regenerate room invite code",
            description = "Generates a new invite code for the specified room, invalidating any previous invite codes. Only room owners can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invite code regenerated successfully",
                    content = @Content(schema = @Schema(implementation = InviteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not the room owner"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @PostMapping("/{id}/invite/regenerate")
    public ResponseEntity<InviteResponse> regenerateInvite(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id
    ) {
        Invite invite = inviteService.generateInvite(id, principal.getUserId(), Instant.now());
        return ResponseEntity.ok(InviteResponse.fromInvite(invite));
    }

    @Operation(
            summary = "List room members",
            description = "Returns a list of user IDs for all members of the specified room. User must be a member of the room."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member list retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Long.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not a member of the room"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found"
            )
    })
    @GetMapping("/{id}/members")
    public ResponseEntity<Set<Long>> getMembers(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(roomService.getMembers(id, principal.getUserId()));
    }

    @Operation(
            summary = "Kick a member from room",
            description = "Removes a member from the specified room. Only the room owner can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Member removed successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not the room owner or trying to kick the owner"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room or member not found"
            )
    })
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Room ID", required = true, example = "123")
            @PathVariable Long id,
            @Parameter(description = "Member ID to remove", required = true, example = "456")
            @PathVariable Long memberId
    ) {
        roomService.removeMember(id, principal.getUserId(), memberId, Instant.now());
        return ResponseEntity.noContent().build();
    }
}