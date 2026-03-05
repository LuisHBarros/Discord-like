package com.luishbarros.discord_like.shared.adapters.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry registry) {
        // Additional metrics configuration can be added here
        // Most configuration is handled via application.yaml
    }
}
