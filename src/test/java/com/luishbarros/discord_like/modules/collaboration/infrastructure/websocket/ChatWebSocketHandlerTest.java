package com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luishbarros.discord_like.modules.collaboration.application.service.MessageService;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.websocket.dto.IncomingMessage;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.shared.ports.PresenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private PresenceStore presenceStore;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private WebSocketSession session;

    private ChatWebSocketHandler handler;

    private static final Long USER_ID = 10L;
    private static final Long ROOM_ID = 20L;
    private static final Long SENDER_ID = 10L;
    private static final String CONTENT = "Hello, World!";
    private static final String CIPHERTEXT = "encrypted_content";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() throws IOException {
        handler = new ChatWebSocketHandler(
                messageService,
                objectMapper,
                sessionManager,
                presenceStore,
                eventPublisher
        );

        // Default mock behaviors
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("test-session-id");
        doNothing().when(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_registersSessionAndSetsOnline() throws IOException {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);

        // Act
        handler.afterConnectionEstablished(session);

        // Assert
        verify(sessionManager).register(session);
        verify(presenceStore).setOnline(USER_ID);
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_withoutUserId_throwsIllegalStateException() throws IOException {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);

        // Act & Assert
        assertThatThrownBy(() -> handler.afterConnectionEstablished(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unauthenticated");

        verify(sessionManager, never()).register(session);
        verify(presenceStore, never()).setOnline(any());
    }

    @Test
    void afterConnectionClosed_setsOfflineAndUnregistersSession() throws IOException {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);

        // Act
        handler.afterConnectionClosed(session, null);

        // Assert
        verify(presenceStore).setOffline(USER_ID);
        verify(sessionManager).unregister(session);
    }

    @Test
    void afterConnectionClosed_withoutUserId_doesNotSetOffline() throws IOException {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);

        // Act
        handler.afterConnectionClosed(session, null);

        // Assert
        verify(presenceStore, never()).setOffline(any());
        verify(sessionManager).unregister(session);
    }

    @Test
    void handleTextMessage_withMessageType_createsAndPublishesMessage() throws Exception {
        // Arrange
        String payload = "{\"type\":\"message\",\"senderId\":" + SENDER_ID + ",\"roomId\":" + ROOM_ID + ",\"content\":\"" + CONTENT + "\"}";
        IncomingMessage incomingMessage = new IncomingMessage("message", ROOM_ID, CONTENT, SENDER_ID);
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("session-id");
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenReturn(incomingMessage);
        when(messageService.createMessage(eq(SENDER_ID), eq(ROOM_ID), eq(CONTENT), any(Instant.class))).thenReturn(message);

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(messageService).createMessage(eq(SENDER_ID), eq(ROOM_ID), eq(CONTENT), any(Instant.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void handleTextMessage_withJoinRoomType_joinsRoom() throws Exception {
        // Arrange
        String payload = "{\"type\":\"join_room\",\"roomId\":" + ROOM_ID + "}";
        IncomingMessage incomingMessage = new IncomingMessage("join_room", ROOM_ID, null, null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenReturn(incomingMessage);

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(sessionManager).joinRoom(eq(ROOM_ID.toString()), eq(session));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_withLeaveRoomType_leavesRoom() throws Exception {
        // Arrange
        String payload = "{\"type\":\"leave_room\",\"roomId\":" + ROOM_ID + "}";
        IncomingMessage incomingMessage = new IncomingMessage("leave_room", ROOM_ID, null, null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenReturn(incomingMessage);

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(sessionManager).leaveRoom(eq(ROOM_ID.toString()), eq(session));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_withPingType_sendsPong() throws Exception {
        // Arrange
        String payload = "{\"type\":\"ping\"}";
        IncomingMessage incomingMessage = new IncomingMessage("ping", null, null, null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenReturn(incomingMessage);

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(session).sendMessage(argThat(msg -> msg.getPayload().equals("{\"type\":\"pong\"}")));
    }

    @Test
    void handleTextMessage_withUnsupportedMessageType_sendsError() throws Exception {
        // Arrange
        String payload = "{\"type\":\"unsupported\"}";
        IncomingMessage incomingMessage = new IncomingMessage("unsupported", null, null, null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, USER_ID);
        when(session.getAttributes()).thenReturn(attributes);
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenReturn(incomingMessage);

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(session).sendMessage(argThat(msg -> ((TextMessage)msg).getPayload().contains("error")));
    }

    @Test
    void handleTextMessage_withUnauthenticatedSession_doesNotCreateMessage() throws Exception {
        // Arrange
        String payload = "{\"type\":\"message\",\"senderId\":" + SENDER_ID + ",\"roomId\":" + ROOM_ID + ",\"content\":\"" + CONTENT + "\"}";

        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(objectMapper.readValue(payload, IncomingMessage.class)).thenThrow(new RuntimeException("Error"));

        TextMessage textMessage = new TextMessage(payload);

        // Act
        handler.handleTextMessage(session, textMessage);

        // Assert
        verify(messageService, never()).createMessage(any(), any(), any(), any());
    }
}