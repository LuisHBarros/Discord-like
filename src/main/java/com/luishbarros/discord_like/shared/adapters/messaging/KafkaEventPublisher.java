package com.luishbarros.discord_like.shared.adapters.messaging;

import com.luishbarros.discord_like.modules.collaboration.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.event.WebSocketDistributionEvent;
import com.luishbarros.discord_like.modules.identity.domain.event.UserEvents;
import com.luishbarros.discord_like.modules.presence.domain.event.PresenceEvents;
import com.luishbarros.discord_like.shared.domain.event.CacheInvalidationEvent;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String DEFAULT_TOPIC = "domain-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Class<?>, String> eventToTopicMap;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventToTopicMap = buildEventToTopicMap();
    }

    @Override
    public void publish(Object event) {
        String topicName = getTopicForEvent(event);
        kafkaTemplate.send(topicName, event);
    }

    private String getTopicForEvent(Object event) {
        String topic = eventToTopicMap.getOrDefault(event.getClass(), DEFAULT_TOPIC);
        log.debug("Publishing event {} to topic {}", event.getClass().getSimpleName(), topic);
        return topic;
    }

    private Map<Class<?>, String> buildEventToTopicMap() {
        return Stream.of(
                Map.entry(RoomEvents.class, "room-events"),
                Map.entry(MessageEvents.class, "message-events"),
                Map.entry(InviteEvents.class, "invite-events"),
                Map.entry(UserEvents.class, "user-events"),
                Map.entry(PresenceEvents.class, "presence-events"),
                Map.entry(WebSocketDistributionEvent.class, "websocket-distribution-events"),
                Map.entry(CacheInvalidationEvent.class, "cache-invalidation-events")
        ).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}