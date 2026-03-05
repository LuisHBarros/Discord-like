package com.luishbarros.discord_like.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    Instant getOccurredAt();
    String getAggregateType();
    String getAggregateId();
}
