package com.luishbarros.discord_like.modules.chat.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.chat.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InviteEventListener implements EventListener<InviteEvents> {

    private static final Logger log = LoggerFactory.getLogger(InviteEventListener.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public InviteEventListener(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(topics = "invite-events", groupId = "discord-like")
    public void onEvent(InviteEvents event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            sessionManager.broadcastToRoom(event.roomId().toString(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize InviteEvent", e);
        } catch (IOException e) {
            log.error("Failed to broadcast InviteEvent to WebSocket sessions", e);
        }
    }
}