package com.luishbarros.discord_like.modules.chat.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto.IncomingMessage;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto.OutgoingMessage;
import com.luishbarros.discord_like.modules.chat.infrastructure.websocket.dto.ErrorResponse;
import com.luishbarros.discord_like.modules.chat.application.service.MessageService;
import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;

    public ChatWebSocketHandler(
            MessageService messageService,
            ObjectMapper objectMapper,
            WebSocketSessionManager sessionManager
    ) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessionManager.register(session);

        // Extract user info from headers
        UUID userId = extractUserId(session);
        session.getAttributes().put("userId", userId);

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
        sessionManager.unregister(session);
    }

    private void handleChatMessage(WebSocketSession session, IncomingMessage payload) throws IOException {
        UUID userId = (UUID) session.getAttributes().get("userId");

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

        OutgoingMessage response = new OutgoingMessage(
                message.getId(),
                payload.roomId(),
                message.getSenderId(),
                message.getCiphertext(),
                message.getCreatedAt()
        );

        sessionManager.broadcastToRoom(
                payload.roomId().toString(),
                objectMapper.writeValueAsString(response)
        );
    }

    private void handleJoinRoom(WebSocketSession session, UUID roomId) throws IOException {
        sessionManager.joinRoom(roomId.toString(), session);
        sendConnectMessage(session, "Joined room: " + roomId);
    }

    private void handleLeaveRoom(WebSocketSession session, UUID roomId) throws IOException {
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
                objectMapper.writeValueAsString(ErrorResponse.of(message, "info"))
        ));
    }

    private UUID extractUserId(WebSocketSession session) {
        if (session.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated WebSocket connection");
        }
        return UUID.fromString(session.getPrincipal().getName());
    }

}