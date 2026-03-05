package com.luishbarros.discord_like.modules.presence.infrastructure.event;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.modules.presence.domain.event.PresenceEvents;
import com.luishbarros.discord_like.shared.ports.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PresenceEventListener implements EventListener<PresenceEvents> {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public PresenceEventListener(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "presence-events", groupId = "discord-like")
    @Override
    public void onEvent(PresenceEvents event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            sessionManager.broadcastToAll(message);
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Error broadcasting presence event: " + e.getMessage());
        }
    }
}
