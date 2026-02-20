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
import java.util.UUID;

@Component
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository jpaRepository;

    public MessageRepositoryAdapter(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Message message) {
        jpaRepository.save(MessageJpaEntity.fromDomain(message));
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id).map(MessageJpaEntity::toDomain);
    }

    @Override
    public Optional<Message> findByIdAndRoomId(UUID messageId, UUID roomId) {
        return jpaRepository.findByIdAndRoomId(messageId, roomId)
                .map(MessageJpaEntity::toDomain);
    }

    @Override
    public List<Message> findByRoomId(UUID roomId) {
        return jpaRepository.findByRoomId(roomId)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Message> findByRoomId(UUID roomId, int limit, int offset) {
        int page = (limit > 0) ? (offset / limit) : 0;
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        return jpaRepository.findByRoomId(roomId, pageable)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Message> findByRoomIdBefore(UUID roomId, UUID beforeMessageId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return jpaRepository.findByRoomIdAndIdBefore(roomId, beforeMessageId, pageable)
                .stream()
                .map(MessageJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByRoomId(UUID roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }

    @Override
    public long countByRoomId(UUID roomId) {
        return jpaRepository.countByRoomId(roomId);
    }
}