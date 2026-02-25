// chat/infrastructure/event/RoomEventListener.java
package com.luishbarros.discord_like.modules.chat.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoomEventListener implements EventListener<RoomEvents> {

    private static final Logger log = LoggerFactory.getLogger(RoomEventListener.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public RoomEventListener(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(topics = "room-events", groupId = "discord-like")
    public void onEvent(RoomEvents event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            sessionManager.broadcastToRoom(event.roomId().toString(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RoomEvent", e);
        } catch (IOException e) {
            log.error("Failed to broadcast RoomEvent to WebSocket sessions", e);
        }
    }
}