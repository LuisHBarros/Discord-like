package com.luishbarros.discord_like.modules.collaboration.infrastructure.event;

import com.luishbarros.discord_like.modules.collaboration.domain.event.MessageEvents;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventListener implements EventListener<MessageEvents> {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);

    @Override
    @KafkaListener(topics = "message-events", groupId = "discord-like")
    public void onEvent(MessageEvents event) {
        // MessageEvents are domain events for persistence and auditing.
        // WebSocket distribution is handled via WebSocketDistributionEvent
        // through WebSocketDistributionEventListener to avoid duplication.
        log.debug("Message event received: messageId={}, roomId={}, senderId={}",
            event.messageId(), event.roomId(), event.senderId());
    }
}
