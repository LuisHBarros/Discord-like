package com.luishbarros.discord_like.modules.auth.infrastructure.http;

import com.luishbarros.discord_like.modules.auth.application.dto.ChangePasswordRequest;
import com.luishbarros.discord_like.modules.auth.application.dto.UserResponse;
import com.luishbarros.discord_like.modules.auth.application.service.AuthService;
import com.luishbarros.discord_like.modules.auth.application.service.UserService;
import com.luishbarros.discord_like.modules.auth.infrastructure.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserResponse response = userService.getById(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.deactivate(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/activate")
    public ResponseEntity<Void> activate(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.activate(principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}