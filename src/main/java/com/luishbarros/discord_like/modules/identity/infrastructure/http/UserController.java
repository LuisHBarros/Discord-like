package com.luishbarros.discord_like.modules.identity.infrastructure.http;

import com.luishbarros.discord_like.modules.identity.application.dto.ChangePasswordRequest;
import com.luishbarros.discord_like.modules.identity.application.dto.UserResponse;
import com.luishbarros.discord_like.modules.identity.application.service.UserService;
import com.luishbarros.discord_like.modules.identity.infrastructure.security.AuthenticatedUser;
import com.luishbarros.discord_like.shared.adapters.ratelimit.RateLimitedAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile and account management operations")
public class UserController {

    private final UserService userService;
    private final RateLimitedAuthService authService;

    public UserController(UserService userService, RateLimitedAuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile information of the authenticated user, including username, email, and active status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized (invalid or missing JWT token)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        UserResponse response = userService.getById(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Change user password",
            description = "Changes the password for the authenticated user. Requires the current password for verification. " +
                    "The new password is hashed using Argon2id before storage."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Password changed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid current password or weak new password"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(
                    description = "Password change request with current and new password",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_password_change",
                                            value = "{\"currentPassword\":\"OldP@ssw0rd123!\",\"newPassword\":\"NewSecureP@ssw0rd456!\"}"
                                    )
                            }
                    )
            )
            @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Deactivate user account",
            description = "Deactivates the authenticated user's account. Deactivated users cannot login until reactivated. " +
                    "This operation is reversible."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Account deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        authService.deactivate(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reactivate user account",
            description = "Reactivates a previously deactivated user account. Allows the user to login again."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Account reactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Account is already active"
            )
    })
    @PatchMapping("/me/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "Authenticated user context", hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        authService.activate(principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}