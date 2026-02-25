package com.luishbarros.discord_like.modules.auth.application.service;

import com.luishbarros.discord_like.modules.auth.application.dto.AuthResponse;
import com.luishbarros.discord_like.modules.auth.application.dto.LoginRequest;
import com.luishbarros.discord_like.modules.auth.application.dto.RegisterRequest;
import com.luishbarros.discord_like.modules.auth.domain.event.UserEvents;
import com.luishbarros.discord_like.modules.auth.domain.model.User;
import com.luishbarros.discord_like.modules.auth.domain.model.error.DuplicateEmailError;
import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidCredentialsError;
import com.luishbarros.discord_like.modules.auth.domain.model.error.UserNotFoundError;
import com.luishbarros.discord_like.modules.auth.domain.ports.PasswordHasher;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import com.luishbarros.discord_like.modules.auth.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final EventPublisher eventPublisher;

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            TokenBlacklist tokenBlacklist,
            EventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.eventPublisher = eventPublisher;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailError(request.email());
        }

        String passwordHash = passwordHasher.hash(request.password());
        Instant now= Instant.now();
        User user = new User(request.username(), request.email(), passwordHash, now);
        User saved = userRepository.save(user);

        eventPublisher.publish(UserEvents.registered(saved, now));

        String accessToken = tokenProvider.generateAccessToken(saved.getId());
        String refreshToken = tokenProvider.generateRefreshToken(saved.getId());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsError("Invalid email or password"));

        if (!passwordHasher.verify(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsError("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        if (tokenBlacklist.isBlacklisted(refreshToken)) {
            throw new InvalidCredentialsError("Token has been revoked");
        }

        Long userId = tokenProvider.validateRefreshToken(refreshToken);
        String newAccessToken = tokenProvider.generateAccessToken(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        tokenBlacklist.add(refreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenBlacklist.add(accessToken);
        tokenBlacklist.add(refreshToken);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundError(userId));

        if (!passwordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsError("Current password is incorrect");
        }

        user.changePassword(passwordHasher.hash(newPassword), Instant.now());
        userRepository.save(user);
    }

    public void deactivate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundError(userId));

        user.deactivate(Instant.now());
        userRepository.save(user);
    }

    public void activate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundError(userId));

        user.activate(Instant.now());
        userRepository.save(user);
    }
}