package com.luishbarros.discord_like.modules.chat.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSessionManagerTest {

    @Test
    void broadcastToRoomRemovesOrphanSessionsAndCleansEmptyRoom() throws IOException {
        WebSocketSessionManager manager = new WebSocketSessionManager();
        Map<String, Set<String>> roomSessions = getRoomSessions(manager);

        roomSessions.computeIfAbsent("room-1", key -> ConcurrentHashMap.newKeySet())
                .add("orphan-session");

        manager.broadcastToRoom("room-1", "hello");

        assertThat(roomSessions).doesNotContainKey("room-1");
    }

    @Test
    void leaveRoomRemovesRoomWhenItBecomesEmpty() {
        WebSocketSessionManager manager = new WebSocketSessionManager();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");

        manager.joinRoom("room-1", session);
        manager.leaveRoom("room-1", session);

        assertThat(getRoomSessions(manager)).doesNotContainKey("room-1");
    }

    @Test
    void unregisterRemovesSessionFromRoomsAndCleansEmptyRoom() {
        WebSocketSessionManager manager = new WebSocketSessionManager();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");

        manager.register(session);
        manager.joinRoom("room-1", session);

        manager.unregister(session);

        assertThat(getRoomSessions(manager)).doesNotContainKey("room-1");
    }

    @Test
    void broadcastToRoomSendsMessageToOpenSession() throws IOException {
        WebSocketSessionManager manager = new WebSocketSessionManager();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        manager.register(session);
        manager.joinRoom("room-1", session);

        manager.broadcastToRoom("room-1", "hello");

        verify(session).sendMessage(argThat(message ->
                message instanceof TextMessage textMessage
                        && "hello".equals(textMessage.getPayload())));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> getRoomSessions(WebSocketSessionManager manager) {
        return (Map<String, Set<String>>) ReflectionTestUtils.getField(manager, "roomSessions");
    }
}
