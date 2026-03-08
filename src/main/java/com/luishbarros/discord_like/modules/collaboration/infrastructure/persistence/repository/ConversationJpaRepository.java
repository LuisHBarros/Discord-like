package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.ConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for ConversationJpaEntity.
 * Provides database access operations for conversations.
 */
@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, Long> {

    /**
     * Find a conversation by its associated room ID.
     *
     * @param roomId the room ID
     * @return Optional containing the conversation if found
     */
    Optional<ConversationJpaEntity> findByRoomId(Long roomId);

    /**
     * Check if a conversation exists for a given room ID.
     *
     * @param roomId the room ID
     * @return true if a conversation exists, false otherwise
     */
    boolean existsByRoomId(Long roomId);

    /**
     * Delete a conversation by its associated room ID.
     *
     * @param roomId the room ID
     */
    void deleteByRoomId(Long roomId);
}
