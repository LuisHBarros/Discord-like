package com.luishbarros.discord_like.modules.chat.infrastructure.adapter;

import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.MessageJpaEntity;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository.MessageJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository jpaRepository;

    public MessageRepositoryAdapter(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity saved = jpaRepository.save(MessageJpaEntity.fromDomain(message));
        return saved.toDomain();
    }

    @Override
    public Optional<Message> findById(Long id) {
        return jpaRepository.findById(id).map(MessageJpaEntity::toDomain);
    }

    @Override
    public Optional<Message> findByIdAndRoomId(Long messageId, Long roomId) {
        return jpaRepository.findByIdAndRoomId(messageId, roomId)
                .map(MessageJpaEntity::toDomain);
    }

    @Override
    public List<Message> findByRoomId(Long roomId) {
        return jpaRepository.findByRoomId(roomId)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Message> findByRoomId(Long roomId, int limit, int offset) {
        int page = (limit > 0) ? (offset / limit) : 0;
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        return jpaRepository.findByRoomId(roomId, pageable)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Message> findByRoomIdBefore(Long roomId, Long beforeMessageId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return jpaRepository.findByRoomIdAndIdBefore(roomId, beforeMessageId, pageable)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }

    @Override
    public long countByRoomId(Long roomId) {
        return jpaRepository.countByRoomId(roomId);
    }
}