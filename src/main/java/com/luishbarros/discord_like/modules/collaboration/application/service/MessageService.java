package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application service for Message operations.
 * This service now delegates message operations to ConversationService
 * while maintaining backwards compatibility with existing controllers.
 * The business logic for message management has moved to the Conversation aggregate
 * and ConversationService.
 */
@Service
@Transactional
public class MessageService {

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final RoomMembershipValidator roomValidator;

    /**
     * Constructor for MessageService.
     *
     * @param conversationService the conversation service for message operations
     * @param messageRepository the message repository for direct access when needed
     * @param roomValidator the room membership validator
     */
    public MessageService(
            ConversationService conversationService,
            MessageRepository messageRepository,
            RoomMembershipValidator roomValidator
    ) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.roomValidator = roomValidator;
    }

    /**
     * Create a new message in a room.
     * Delegates to ConversationService for message creation with limit enforcement.
     *
     * @param senderId the sender's user ID
     * @param roomId the room ID
     * @param content the message content
     * @param now the current timestamp
     * @return the created message
     */
    public Message createMessage(Long senderId, Long roomId, String content, Instant now) {
        return conversationService.addMessage(roomId, senderId, content, now);
    }

    /**
     * Find messages by room ID with pagination.
     *
     * @param roomId the room ID
     * @param userId the user ID requesting the messages
     * @param limit the maximum number of messages to return
     * @param offset the number of messages to skip
     * @return list of messages
     */
    public List<Message> findByRoomId(Long roomId, Long userId, int limit, int offset) {
        // Validate user is a member of the room
        roomValidator.validateAndGetRoom(roomId, userId);
        return conversationService.getMessages(roomId, limit, offset);
    }

    /**
     * Find messages by room ID before a specific message (cursor-based pagination).
     *
     * @param roomId the room ID
     * @param userId the user ID requesting the messages
     * @param beforeMessageId the message ID to get messages before
     * @param limit the maximum number of messages to return
     * @return list of messages
     */
    public List<Message> findByRoomIdBefore(Long roomId, Long userId, Long beforeMessageId, int limit) {
        // Validate user is a member of the room
        roomValidator.validateAndGetRoom(roomId, userId);
        return conversationService.getMessagesBefore(roomId, beforeMessageId, limit);
    }

    /**
     * Update an existing message.
     * Delegates to ConversationService which enforces ownership through the Conversation aggregate.
     *
     * @param senderId the user ID attempting to edit
     * @param roomId the room ID
     * @param messageId the message ID to edit
     * @param plaintext the new message content
     * @param now the current timestamp
     * @return the updated message
     */
    public Message updateMessage(Long senderId, Long roomId, Long messageId, String plaintext, Instant now) {
        return conversationService.updateMessage(roomId, senderId, messageId, plaintext, now);
    }

    /**
     * Delete a message.
     * Delegates to ConversationService which enforces ownership through the Conversation aggregate.
     *
     * @param senderId the user ID attempting to delete
     * @param roomId the room ID
     * @param messageId the message ID to delete
     */
    public void deleteMessage(Long senderId, Long roomId, Long messageId) {
        conversationService.deleteMessage(roomId, senderId, messageId, Instant.now());
    }

    /**
     * Find a message by ID and room ID.
     * This is a direct repository access for backward compatibility.
     *
     * @param messageId the message ID
     * @param roomId the room ID
     * @return the message if found
     */
    public java.util.Optional<Message> findByIdAndRoomId(Long messageId, Long roomId) {
        return messageRepository.findByIdAndRoomId(messageId, roomId);
    }

    /**
     * Count messages in a room.
     *
     * @param roomId the room ID
     * @return the number of messages
     */
    public long countByRoomId(Long roomId) {
        return messageRepository.countByRoomId(roomId);
    }
}