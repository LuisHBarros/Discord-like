package com.luishbarros.discord_like.modules.chat.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto.OutgoingMessage;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class MessageEventListener implements EventListener<MessageEvents> {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public MessageEventListener(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(topics = "message-events", groupId = "discord-like")
    public void onEvent(MessageEvents event) {
        try {
            OutgoingMessage payload = new OutgoingMessage(
                    event.messageId(),
                    event.roomId(),
                    event.senderId(),
                    event.ciphertext(),
                    event.editedAt() != null ? event.editedAt() : Instant.now()
            );
            String json = objectMapper.writeValueAsString(payload);
            sessionManager.broadcastToRoom(event.roomId().toString(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MessageEvent for WebSocket broadcast", e);
        } catch (IOException e) {
            log.error("Failed to broadcast MessageEvent to WebSocket sessions", e);
        }
    }
}