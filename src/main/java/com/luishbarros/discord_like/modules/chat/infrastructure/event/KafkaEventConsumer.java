package com.luishbarros.discord_like.modules.chat.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.chat.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto.OutgoingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public KafkaEventConsumer(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "message-events", groupId = "discord-like")
    public void onMessageEvent(MessageEvents event) {
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

    @KafkaListener(topics = "room-events", groupId = "discord-like")
    public void onRoomEvent(RoomEvents event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            sessionManager.broadcastToRoom(event.roomId().toString(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RoomEvent", e);
        } catch (IOException e) {
            log.error("Failed to broadcast RoomEvent to WebSocket sessions", e);
        }
    }

    @KafkaListener(topics = "invite-events", groupId = "discord-like")
    public void onInviteEvent(InviteEvents event) {
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
