package com.luishbarros.discord_like.modules.collaboration.domain.ports.repository;

import com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation;

import java.util.Optional;

/**
 * Repository port for managing Conversation aggregates.
 * This defines the contract for persistence operations on conversations.
 */
public interface ConversationRepository {

    /**
     * Find a conversation by its associated room ID.
     *
     * @param roomId the room ID to search for
     * @return Optional containing the conversation if found
     */
    Optional<Conversation> findByRoomId(Long roomId);

    /**
     * Save a conversation aggregate.
     * This will either create a new conversation or update an existing one.
     *
     * @param conversation the conversation to save
     * @return the saved conversation with generated ID
     */
    Conversation save(Conversation conversation);

    /**
     * Find a conversation by its ID.
     *
     * @param id the conversation ID
     * @return Optional containing the conversation if found
     */
    Optional<Conversation> findById(Long id);

    /**
     * Delete a conversation by its ID.
     *
     * @param id the conversation ID to delete
     */
    void deleteById(Long id);

    /**
     * Delete a conversation by its associated room ID.
     *
     * @param roomId the room ID
     */
    void deleteByRoomId(Long roomId);
}
