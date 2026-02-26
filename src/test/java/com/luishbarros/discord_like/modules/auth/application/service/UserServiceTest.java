package com.luishbarros.discord_like.modules.auth.application.service;

import com.luishbarros.discord_like.modules.auth.application.dto.UserResponse;
import com.luishbarros.discord_like.modules.auth.domain.model.User;
import com.luishbarros.discord_like.modules.auth.domain.model.error.UserNotFoundError;
import com.luishbarros.discord_like.modules.auth.domain.ports.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final Long   USER_ID  = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL    = "test@example.com";

    private User user() {
        return User.reconstitute(USER_ID, USERNAME, EMAIL, "hash", true, Instant.now(), Instant.now());
    }

    // ─── GetById ──────────────────────────────────────────────────────────────

    @Nested
    class GetById {

        @Test
        void givenExistingId_returnsMatchingUserResponse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

            UserResponse response = userService.getById(USER_ID);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.email()).isEqualTo(EMAIL);
        }

        @Test
        void givenUnknownId_throwsUserNotFoundError() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getById(USER_ID))
                    .isInstanceOf(UserNotFoundError.class);
        }
    }

    // ─── GetByEmail ───────────────────────────────────────────────────────────

    @Nested
    class GetByEmail {

        @Test
        void givenExistingEmail_returnsMatchingUserResponse() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

            UserResponse response = userService.getByEmail(EMAIL);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.email()).isEqualTo(EMAIL);
        }

        @Test
        void givenUnknownEmail_throwsUserNotFoundError() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getByEmail(EMAIL))
                    .isInstanceOf(UserNotFoundError.class);
        }
    }
}
