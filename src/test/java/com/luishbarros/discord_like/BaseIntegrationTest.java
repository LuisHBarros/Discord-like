package com.luishbarros.discord_like;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base integration test configuration using Testcontainers.
 * It imports the TestcontainersConfiguration which provisions PostgreSQL, Redis, and Kafka.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {

}
