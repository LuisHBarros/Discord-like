package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.ConnectResponse;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.ErrorResponse;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.IncomingMessage;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.OutgoingMessage;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.event.WebSocketDistributionEvent;
import com.luishbarros.discord_like.modules.collaboration.application.service.MessageService;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.shared.ports.PresenceStore;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final PresenceStore presenceStore;
    private final EventPublisher eventPublisher;
    public static final String ATTR_USER_ID = "userId";

    public ChatWebSocketHandler(
            MessageService messageService,
            ObjectMapper objectMapper,
            WebSocketSessionManager sessionManager,
            PresenceStore presenceStore,
            EventPublisher eventPublisher
    ) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
        this.presenceStore = presenceStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessionManager.register(session);

        // Extract user info from headers
        Long userId = extractUserId(session);
        session.getAttributes().put(ATTR_USER_ID, userId);
        presenceStore.setOnline(userId);
        sendConnectMessage(session, "Connected to chat server");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            var payload = objectMapper.readValue(message.getPayload(), IncomingMessage.class);

            switch (payload.type()) {
                case "message" -> handleChatMessage(session, payload);
                case "join_room" -> handleJoinRoom(session, payload.roomId());
                case "leave_room" -> handleLeaveRoom(session, payload.roomId());
                case "ping" -> session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                default -> sendError(session, "unsupported_event");
            }
        } catch (Exception e) {
            sendError(session, "invalid_message");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId != null) {
            presenceStore.setOffline(userId);
        }
        sessionManager.unregister(session);
    }

    private void handleChatMessage(WebSocketSession session, IncomingMessage payload) throws IOException {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);

        if (!userId.equals(payload.senderId())) {
            sendError(session, "unauthorized");
            return;
        }

        Message message = messageService.createMessage(
                payload.senderId(),
                payload.roomId(),
                payload.content(),
                Instant.now()
        );

        // Publish WebSocket distribution event for cross-node broadcasting
        WebSocketDistributionEvent distributionEvent = WebSocketDistributionEvent.chatMessage(
            payload.roomId(),
            payload.senderId(),
            message.getContent().ciphertext(),
            message.getCreatedAt()
        );
        eventPublisher.publish(distributionEvent);
    }

    private void handleJoinRoom(WebSocketSession session, Long roomId) throws IOException {
        sessionManager.joinRoom(roomId.toString(), session);
        sendConnectMessage(session, "Joined room: " + roomId);
    }

    private void handleLeaveRoom(WebSocketSession session, Long roomId) throws IOException {
        sessionManager.leaveRoom(roomId.toString(), session);
        sendConnectMessage(session, "Left room: " + roomId);
    }

    private void sendError(WebSocketSession session, String code) throws IOException {
        session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(ErrorResponse.of(code, "error"))
        ));
    }

    private void sendConnectMessage(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(ConnectResponse.of(message))
        ));
    }

    private Long extractUserId(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            throw new IllegalStateException("Unauthenticated WebSocket connection");
        }
        return userId;
    }

}