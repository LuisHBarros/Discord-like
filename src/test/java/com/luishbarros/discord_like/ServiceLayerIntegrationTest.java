package com.luishbarros.discord_like;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class ServiceLayerIntegrationTest {

	@Test
	void contextLoadsForService() {
		// Test application services here
		// The test environment spins up Postgres, Redis, and Kafka automatically
	}

}
