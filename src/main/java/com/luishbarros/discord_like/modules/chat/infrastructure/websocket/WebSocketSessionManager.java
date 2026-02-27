package com.luishbarros.discord_like.modules.chat.infrastructure.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        allSessions.put(session.getId(), session);
    }

    public void unregister(WebSocketSession session) {
        String sessionId = session.getId();
        allSessions.remove(sessionId);

        // Remove from all rooms
        roomSessions.forEach((roomId, sessions) -> {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId, sessions);
            }
        });
    }

    public void joinRoom(String roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
    }

    public void leaveRoom(String roomId, WebSocketSession session) {
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session.getId());
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId, sessions);
        }
    }

    public void broadcastToRoom(String roomId, String message) throws IOException {
        Set<String> sessions = roomSessions.getOrDefault(roomId, Collections.emptySet());
        if (sessions.isEmpty()) {
            return;
        }

        for (String sessionId : List.copyOf(sessions)) {
            WebSocketSession session = allSessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                sessions.remove(sessionId);
                continue;
            }

            session.sendMessage(new TextMessage(message));
        }

        if (sessions.isEmpty()) {
            roomSessions.remove(roomId, sessions);
        }
    }

    public void sendToSession(String sessionId, String message) throws IOException {
        WebSocketSession session = allSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }
}
