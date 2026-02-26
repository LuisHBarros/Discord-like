package com.luishbarros.discord_like.modules.auth.application.service;

import com.luishbarros.discord_like.modules.auth.application.dto.AuthResponse;
import com.luishbarros.discord_like.modules.auth.application.dto.LoginRequest;
import com.luishbarros.discord_like.modules.auth.application.dto.RegisterRequest;
import com.luishbarros.discord_like.modules.auth.domain.event.UserEvents;
import com.luishbarros.discord_like.modules.auth.domain.model.User;
import com.luishbarros.discord_like.modules.auth.domain.model.error.DuplicateEmailError;
import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidCredentialsError;
import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidUserError;
import com.luishbarros.discord_like.modules.auth.domain.model.error.UserNotFoundError;
import com.luishbarros.discord_like.modules.auth.domain.ports.PasswordHasher;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import com.luishbarros.discord_like.modules.auth.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenProvider tokenProvider;
    @Mock private TokenBlacklist tokenBlacklist;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private static final Long   USER_ID        = 1L;
    private static final String USERNAME       = "testuser";
    private static final String EMAIL          = "test@example.com";
    private static final String PASSWORD       = "password123";
    private static final String HASHED         = "$argon2id$hashed";
    private static final String ACCESS_TOKEN   = "access.token";
    private static final String REFRESH_TOKEN  = "refresh.token";

    private User activeUser() {
        return User.reconstitute(USER_ID, USERNAME, EMAIL, HASHED, true, Instant.now(), Instant.now());
    }

    private User inactiveUser() {
        return User.reconstitute(USER_ID, USERNAME, EMAIL, HASHED, false, Instant.now(), Instant.now());
    }

    // ─── Register ────────────────────────────────────────────────────────────

    @Nested
    class Register {

        @Test
        void givenNewEmail_returnsTokenPair() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD)).thenReturn(HASHED);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                return User.reconstitute(USER_ID, u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), true, u.getCreatedAt(), u.getUpdatedAt());
            });
            when(tokenProvider.generateAccessToken(USER_ID)).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            AuthResponse response = authService.register(new RegisterRequest(USERNAME, EMAIL, PASSWORD));

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        void givenNewEmail_savesUserWithHashedPassword() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD)).thenReturn(HASHED);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                return User.reconstitute(USER_ID, u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), true, u.getCreatedAt(), u.getUpdatedAt());
            });
            when(tokenProvider.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(any())).thenReturn(REFRESH_TOKEN);

            authService.register(new RegisterRequest(USERNAME, EMAIL, PASSWORD));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo(HASHED);
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        void givenNewEmail_publishesRegisteredEvent() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD)).thenReturn(HASHED);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                return User.reconstitute(USER_ID, u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), true, u.getCreatedAt(), u.getUpdatedAt());
            });
            when(tokenProvider.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(any())).thenReturn(REFRESH_TOKEN);

            authService.register(new RegisterRequest(USERNAME, EMAIL, PASSWORD));

            ArgumentCaptor<UserEvents> captor = ArgumentCaptor.forClass(UserEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(UserEvents.EventType.REGISTERED);
            assertThat(captor.getValue().email()).isEqualTo(EMAIL);
            assertThat(captor.getValue().username()).isEqualTo(USERNAME);
        }

        @Test
        void givenDuplicateEmail_throwsDuplicateEmailError() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(new RegisterRequest(USERNAME, EMAIL, PASSWORD)))
                    .isInstanceOf(DuplicateEmailError.class);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Nested
    class Login {

        @Test
        void givenValidCredentials_returnsTokenPair() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));
            when(passwordHasher.verify(PASSWORD, HASHED)).thenReturn(true);
            when(tokenProvider.generateAccessToken(USER_ID)).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            AuthResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        void givenUnknownEmail_throwsInvalidCredentialsError() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(InvalidCredentialsError.class);
        }

        @Test
        void givenWrongPassword_throwsInvalidCredentialsError() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));
            when(passwordHasher.verify(PASSWORD, HASHED)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(InvalidCredentialsError.class);
        }
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    @Nested
    class Refresh {

        @Test
        void givenValidToken_returnsNewTokenPairAndBlacklistsOldRefreshToken() {
            String newRefresh = "new.refresh.token";
            when(tokenBlacklist.isBlacklisted(REFRESH_TOKEN)).thenReturn(false);
            when(tokenProvider.validateRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(tokenProvider.generateAccessToken(USER_ID)).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(USER_ID)).thenReturn(newRefresh);

            AuthResponse response = authService.refresh(REFRESH_TOKEN);

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(newRefresh);
            verify(tokenBlacklist).add(REFRESH_TOKEN);
        }

        @Test
        void givenBlacklistedToken_throwsInvalidCredentialsError() {
            when(tokenBlacklist.isBlacklisted(REFRESH_TOKEN)).thenReturn(true);

            assertThatThrownBy(() -> authService.refresh(REFRESH_TOKEN))
                    .isInstanceOf(InvalidCredentialsError.class);

            verify(tokenProvider, never()).validateRefreshToken(any());
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Nested
    class Logout {

        @Test
        void blacklistsBothAccessAndRefreshTokens() {
            authService.logout(ACCESS_TOKEN, REFRESH_TOKEN);

            verify(tokenBlacklist).add(ACCESS_TOKEN);
            verify(tokenBlacklist).add(REFRESH_TOKEN);
        }
    }

    // ─── ChangePassword ───────────────────────────────────────────────────────

    @Nested
    class ChangePassword {

        private static final String NEW_PASSWORD = "newpassword123";
        private static final String NEW_HASH     = "$argon2id$newhash";

        @Test
        void givenCorrectCurrentPassword_savesNewHash() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
            when(passwordHasher.verify(PASSWORD, HASHED)).thenReturn(true);
            when(passwordHasher.hash(NEW_PASSWORD)).thenReturn(NEW_HASH);

            authService.changePassword(USER_ID, PASSWORD, NEW_PASSWORD);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo(NEW_HASH);
        }

        @Test
        void givenUnknownUser_throwsUserNotFoundError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(USER_ID, PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(UserNotFoundError.class);
        }

        @Test
        void givenWrongCurrentPassword_throwsInvalidCredentialsError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
            when(passwordHasher.verify(PASSWORD, HASHED)).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(USER_ID, PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidCredentialsError.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void givenInactiveUser_throwsInvalidUserError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(inactiveUser()));
            when(passwordHasher.verify(PASSWORD, HASHED)).thenReturn(true);
            when(passwordHasher.hash(NEW_PASSWORD)).thenReturn(NEW_HASH);

            assertThatThrownBy(() -> authService.changePassword(USER_ID, PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidUserError.class);
        }
    }

    // ─── Deactivate ───────────────────────────────────────────────────────────

    @Nested
    class Deactivate {

        @Test
        void givenActiveUser_savesDeactivatedUser() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

            authService.deactivate(USER_ID);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        void givenUnknownUser_throwsUserNotFoundError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.deactivate(USER_ID))
                    .isInstanceOf(UserNotFoundError.class);
        }

        @Test
        void givenAlreadyInactiveUser_throwsInvalidUserError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(inactiveUser()));

            assertThatThrownBy(() -> authService.deactivate(USER_ID))
                    .isInstanceOf(InvalidUserError.class);
        }
    }

    // ─── Activate ─────────────────────────────────────────────────────────────

    @Nested
    class Activate {

        @Test
        void givenInactiveUser_savesActivatedUser() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(inactiveUser()));

            authService.activate(USER_ID);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        void givenUnknownUser_throwsUserNotFoundError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.activate(USER_ID))
                    .isInstanceOf(UserNotFoundError.class);
        }

        @Test
        void givenAlreadyActiveUser_throwsInvalidUserError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

            assertThatThrownBy(() -> authService.activate(USER_ID))
                    .isInstanceOf(InvalidUserError.class);
        }
    }
}
