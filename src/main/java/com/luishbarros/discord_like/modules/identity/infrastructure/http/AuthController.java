package com.luishbarros.discord_like.modules.identity.infrastructure.http;

import com.luishbarros.discord_like.modules.identity.application.dto.AuthResponse;
import com.luishbarros.discord_like.modules.identity.application.dto.LoginRequest;
import com.luishbarros.discord_like.modules.identity.application.dto.RefreshRequest;
import com.luishbarros.discord_like.modules.identity.application.dto.RegisterRequest;
import com.luishbarros.discord_like.shared.adapters.ratelimit.RateLimitedAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and authorization operations")
public class AuthController {

    private final RateLimitedAuthService authService;

    public AuthController(RateLimitedAuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with username, email, and password. " +
                    "The password is hashed using Argon2id before storage. " +
                    "Returns JWT access and refresh tokens on success."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data (email format, password strength, etc.)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (5 requests per 60 seconds per IP)"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Parameter(
                    description = "User registration details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_registration",
                                            value = "{\"username\":\"john_doe\",\"email\":\"john.doe@example.com\",\"password\":\"SecureP@ssw0rd123!\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody RegisterRequest request,
            @Parameter(
                    description = "Client IP address for rate limiting",
                    hidden = true
            )
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "User login",
            description = "Authenticates a user with email and password. " +
                    "Returns JWT access and refresh tokens on successful authentication. " +
                    "Access tokens expire in 15 minutes, refresh tokens in 7 days."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials (wrong email or password)"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(
                    description = "User login credentials",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_login",
                                            value = "{\"email\":\"john.doe@example.com\",\"password\":\"SecureP@ssw0rd123!\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody LoginRequest request,
            @Parameter(
                    description = "Client IP address for rate limiting",
                    hidden = true
            )
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Obtains a new access token using a valid refresh token. " +
                    "Refresh tokens are valid for 7 days."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(
                    description = "Refresh token",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_refresh",
                                            value = "{\"refreshToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjk5NzI3NjAsfQ\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody RefreshRequest request
    ) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "User logout",
            description = "Invalidates the current access and refresh tokens by adding them to the blacklist. " +
                    "Requires both access token (in Authorization header) and refresh token (in body)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Logout successful"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid access token"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing refresh token or invalid Authorization header format"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(
                    description = "JWT access token (Bearer token)",
                    required = true,
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String accessToken,
            @Parameter(
                    description = "Refresh token to invalidate",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "valid_refresh",
                                            value = "{\"refreshToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjk5NzI3NjAsfQ\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(extractToken(accessToken), request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}