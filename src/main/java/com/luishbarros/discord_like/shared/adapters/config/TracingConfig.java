package com.luishbarros.discord_like.shared.adapters.config;

import brave.Tracing;
import brave.propagation.B3Propagation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;

@Configuration
public class TracingConfig {

    @Bean
    public Tracing braveTracing() {
        return Tracing.newBuilder()
                .localServiceName("discord-like")
                .traceId128Bit(true)
                .sampler(brave.sampler.Sampler.create(0.1f))
                .propagationFactory(B3Propagation.FACTORY)
                .build();
    }
}
