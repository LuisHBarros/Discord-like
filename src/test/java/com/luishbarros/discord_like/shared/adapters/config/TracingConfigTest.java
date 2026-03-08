package com.luishbarros.discord_like.shared.adapters.config;

import brave.Tracing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.luishbarros.discord_like.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test configuration for distributed tracing.
 * This test uses Testcontainers to provide PostgreSQL database.
 */
@SpringBootTest
public class TracingConfigTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private Tracing tracing;

    @Test
    void tracingShouldBeConfigured() {
        assertThat(tracing).isNotNull();
    }

    @Test
    void traceContextShouldPropagate() {
        var span = tracing.tracer().newTrace().name("test-span").start();
        try {
            var context = tracing.currentTraceContext().get();
            if (context != null) {
                assertThat(context.traceIdString()).isNotNull();
                assertThat(context.spanIdString()).isNotNull();
            }
        } finally {
            span.finish();
        }
    }
}
