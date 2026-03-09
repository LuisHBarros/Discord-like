package com.luishbarros.discord_like;

import com.luishbarros.discord_like.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test to verify Spring application context loads successfully.
 * This test uses Testcontainers to provide PostgreSQL database for context loading.
 */
@Tag("integration")
@SpringBootTest
public class DiscordLikeApplicationTests extends BaseIntegrationTest {

	@Test
	void contextLoads() {
	}

}
