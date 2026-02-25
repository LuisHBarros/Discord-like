package com.luishbarros.discord_like.shared.adapters.messaging;

import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Object event) {
        String topicName = getTopicForEvent(event);
        kafkaTemplate.send(topicName, event);
    }

    private String getTopicForEvent(Object event) {
        return switch (event.getClass().getSimpleName()) {
            case "RoomEvents"     -> "room-events";
            case "MessageEvents"  -> "message-events";
            case "InviteEvents"   -> "invite-events";
            case "UserEvents"     -> "user-events";
            default               -> "domain-events";
        };
    }
}