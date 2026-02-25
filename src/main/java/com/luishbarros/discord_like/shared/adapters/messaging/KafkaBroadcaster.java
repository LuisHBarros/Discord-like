// shared/adapters/messaging/KafkaBroadcaster.java
package com.luishbarros.discord_like.shared.adapters.messaging;

import com.luishbarros.discord_like.shared.ports.Broadcaster;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaBroadcaster implements Broadcaster {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaBroadcaster(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void broadcast(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }
}