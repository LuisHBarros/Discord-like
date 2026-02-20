package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    void save(Message message);

    Optional<Message> findById(UUID id);

    Optional<Message> findByIdAndRoomId(UUID messageId, UUID roomId);

    List<Message> findByRoomId(UUID roomId);

    // Standard pagination
    List<Message> findByRoomId(UUID roomId, int limit, int offset);

    // Cursor-based pagination (Better for Chat apps)
    List<Message> findByRoomIdBefore(UUID roomId, UUID beforeMessageId, int limit);

    void deleteById(UUID id);

    void deleteByRoomId(UUID roomId);

    long countByRoomId(UUID roomId);
}