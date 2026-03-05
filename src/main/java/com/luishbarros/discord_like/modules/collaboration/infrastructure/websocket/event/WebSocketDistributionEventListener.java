package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.WebSocketSessionManager;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.OutgoingMessage;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebSocketDistributionEventListener implements EventListener<WebSocketDistributionEvent> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDistributionEventListener.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public WebSocketDistributionEventListener(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(topics = "websocket-distribution-events", groupId = "discord-like-websocket")
    public void onEvent(WebSocketDistributionEvent event) {
        switch (event.messageType()) {
            case CHAT_MESSAGE -> deliverChatMessage(event);
            case ROOM_JOIN -> deliverRoomJoin(event);
            case ROOM_LEAVE -> deliverRoomLeave(event);
        }
    }

    private void deliverChatMessage(WebSocketDistributionEvent event) {
        try {
            // Reuse existing OutgoingMessage DTO for consistency
            OutgoingMessage payload = new OutgoingMessage(
                    null, // messageId not available at this level
                    event.roomId(),
                    event.senderId(),
                    event.content(),
                    event.createdAt()
            );
            String json = objectMapper.writeValueAsString(payload);
            sessionManager.broadcastToRoom(event.roomId().toString(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WebSocketDistributionEvent for broadcast", e);
        } catch (IOException e) {
            log.error("Failed to broadcast WebSocketDistributionEvent to WebSocket sessions", e);
        }
    }

    private void deliverRoomJoin(WebSocketDistributionEvent event) {
        // Future: Handle room join broadcasts
        log.debug("Room join event received: roomId={}, senderId={}", event.roomId(), event.senderId());
    }

    private void deliverRoomLeave(WebSocketDistributionEvent event) {
        // Future: Handle room leave broadcasts
        log.debug("Room leave event received: roomId={}, senderId={}", event.roomId(), event.senderId());
    }
}
