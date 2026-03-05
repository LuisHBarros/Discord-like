package com.luishbarros.discord_like.shared.adapters.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "e2ee.enabled", havingValue = "true", matchIfMissing = false)
public class E2EEFeatureConfig {
    // E2EE beans will only be created when enabled
}
