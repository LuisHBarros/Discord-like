package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.ConversationRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.ConversationJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.ConversationJpaRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.MessageJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementation for ConversationRepository port.
 * Bridges domain logic with JPA persistence infrastructure.
 */
@Component
@Transactional
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository conversationJpaRepository;
    private final MessageJpaRepository messageJpaRepository;

    public ConversationRepositoryAdapter(
            ConversationJpaRepository conversationJpaRepository,
            MessageJpaRepository messageJpaRepository
    ) {
        this.conversationJpaRepository = conversationJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
    }

    @Override
    public Optional<Conversation> findByRoomId(Long roomId) {
        return conversationJpaRepository.findByRoomId(roomId)
            .map(entity -> {
                // Load messages separately to avoid N+1 queries
                List<Message> messages = messageJpaRepository.findByRoomId(roomId)
                    .stream()
                    .map(msgEntity -> msgEntity.toDomain())
                    .toList();
                return entity.toDomain(messages);
            });
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity entity = ConversationJpaEntity.fromDomain(conversation);
        ConversationJpaEntity saved = conversationJpaRepository.save(entity);

        // Messages are saved separately through MessageRepository
        // Return the conversation with the generated ID
        if (conversation.getId() == null) {
            // For new conversations, create a new instance with the generated ID
            return Conversation.reconstitute(
                saved.getId(),
                saved.getRoomId(),
                conversation.getMessages(),
                conversation.getLastActivityAt()
            );
        }

        // For existing conversations, load messages and reconstitute
        List<Message> messages = messageJpaRepository.findByRoomId(conversation.getRoomId())
            .stream()
            .map(msgEntity -> msgEntity.toDomain())
            .toList();

        return saved.toDomain(messages);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return conversationJpaRepository.findById(id)
            .map(entity -> {
                // Load messages separately
                List<Message> messages = messageJpaRepository.findByRoomId(entity.getRoomId())
                    .stream()
                    .map(msgEntity -> msgEntity.toDomain())
                    .toList();
                return entity.toDomain(messages);
            });
    }

    @Override
    public void deleteById(Long id) {
        conversationJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        conversationJpaRepository.deleteByRoomId(roomId);
    }
}
