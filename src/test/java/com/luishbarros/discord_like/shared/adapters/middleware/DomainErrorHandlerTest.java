package com.luishbarros.discord_like.shared.adapters.middleware;

import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidUserError;
import com.luishbarros.discord_like.modules.chat.domain.error.EncryptionException;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidInviteCodeError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.shared.domain.error.DomainError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DomainErrorHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new DomainErrorHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void methodArgumentNotValidReturnsStandardValidationPayload() throws Exception {
        String body = """
                {
                  "name": "",
                  "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("name: Name is required")))
                .andExpect(jsonPath("$.message", containsString("email: Invalid email format")))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @ParameterizedTest
    @MethodSource("badRequestDomainErrorCases")
    void badRequestDomainErrorsMapToStatus400(String path, String code) throws Exception {
        mockMvc.perform(get("/test/errors/domain/{path}", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(code))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    private static Stream<Arguments> badRequestDomainErrorCases() {
        return Stream.of(
                arguments("invalid-message", "INVALID_MESSAGE"),
                arguments("invalid-room", "INVALID_ROOM"),
                arguments("invalid-user", "INVALID_USER"),
                arguments("invalid-invite-code", "INVALID_INVITE_CODE"),
                arguments("encryption-error", "ENCRYPTION_ERROR")
        );
    }

    @RestController
    @RequestMapping("/test/errors")
    static class TestController {

        @PostMapping("/validation")
        void validation(@Valid @RequestBody ValidationRequest request) {
            // validation endpoint for testing exception mapping
        }

        @GetMapping("/domain/{path}")
        void domainError(@PathVariable String path) {
            throw mapError(path);
        }

        private DomainError mapError(String path) {
            return switch (path) {
                case "invalid-message" -> InvalidMessageError.emptyContent();
                case "invalid-room" -> new InvalidRoomError("Invalid room");
                case "invalid-user" -> new InvalidUserError("Invalid user");
                case "invalid-invite-code" -> InvalidInviteCodeError.emptyCodeValue();
                case "encryption-error" -> new EncryptionException("Encryption failed");
                default -> new InvalidMessageError("Unexpected path for test");
            };
        }
    }

    record ValidationRequest(
            @NotBlank(message = "Name is required")
            String name,
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}
}
