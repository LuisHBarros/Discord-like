package com.luishbarros.discord_like.modules.chat.infrastructure.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
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
        roomSessions.forEach((roomId, sessions) -> sessions.remove(sessionId));
    }

    public void joinRoom(String roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
    }

    public void leaveRoom(String roomId, WebSocketSession session) {
        roomSessions.getOrDefault(roomId, new HashSet<>())
                .remove(session.getId());
    }

    public void broadcastToRoom(String roomId, String message) throws IOException {
        Set<String> sessions = roomSessions.getOrDefault(roomId, new HashSet<>());

        for (String sessionId : sessions) {
            WebSocketSession session = allSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            }
        }
    }

    public void sendToSession(String sessionId, String message) throws IOException {
        WebSocketSession session = allSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new org.springframework.web.socket.TextMessage(message));
        }
    }
}