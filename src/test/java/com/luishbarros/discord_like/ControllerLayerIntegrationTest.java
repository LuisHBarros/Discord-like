package com.luishbarros.discord_like;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import com.luishbarros.discord_like.modules.collaboration.application.service.E2EEKeyManagementService;
import com.luishbarros.discord_like.modules.collaboration.application.service.InviteService;
import com.luishbarros.discord_like.modules.collaboration.application.service.MessageService;
import com.luishbarros.discord_like.modules.collaboration.application.service.RoomService;
import com.luishbarros.discord_like.modules.identity.application.service.AuthService;
import com.luishbarros.discord_like.modules.identity.application.service.UserService;
import com.luishbarros.discord_like.modules.identity.domain.ports.PasswordHasher;
import com.luishbarros.discord_like.modules.identity.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.identity.domain.ports.TokenProvider;
import com.luishbarros.discord_like.modules.identity.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.modules.presence.application.service.PresenceService;
import com.luishbarros.discord_like.shared.adapters.http.HealthController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller layer unit test using @WebMvcTest.
 * Uses mocks for service dependencies and doesn't require Testcontainers.
 */
@WebMvcTest(controllers = HealthController.class,
		excludeAutoConfiguration = {
			org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
			org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
			org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class,
			org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration.class,
			org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
			org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
		})
@TestPropertySource(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration"
})
public class ControllerLayerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Mock

	private RoomService roomService;

	@Mock
	private MessageService messageService;

	@Mock
	private InviteService inviteService;

    @Mock
	private E2EEKeyManagementService e2eeKeyManagementService;

    @Mock
	private AuthService authService;

    @Mock
	private UserService userService;

    @Mock
	private PresenceService presenceService;

    @Mock
	private TokenProvider tokenProvider;

    @Mock
	private PasswordHasher passwordHasher;

    @Mock
	private TokenBlacklist tokenBlacklist;

    @Mock
	private UserRepository userRepository;

    @Mock
	private Tracing tracing;

    @Mock
	private CurrentTraceContext currentTraceContext;

	@Test
	void healthEndpointShouldReturnOk() throws Exception {
		// Set up the Tracing mock to avoid NullPointerException in TracingInterceptor
		when(tracing.currentTraceContext()).thenReturn(currentTraceContext);
		when(currentTraceContext.get()).thenReturn(null);

		mockMvc.perform(get("/health"))
				.andExpect(status().isOk());
	}

}