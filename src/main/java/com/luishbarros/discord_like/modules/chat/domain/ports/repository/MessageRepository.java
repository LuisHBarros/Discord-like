package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {
    void save(Message message);

    Optional<Message> findById(Long id);

    Optional<Message> findByIdAndRoomId(Long messageId, Long roomId);

    List<Message> findByRoomId(Long roomId);

    // Standard pagination
    List<Message> findByRoomId(Long roomId, int limit, int offset);

    // Cursor-based pagination (Better for Chat apps)
    List<Message> findByRoomIdBefore(Long roomId, Long beforeMessageId, int limit);

    void deleteById(Long id);

    void deleteByRoomId(Long roomId);

    long countByRoomId(Long roomId);
}