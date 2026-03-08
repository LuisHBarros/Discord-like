package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.collaboration.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.ConversationRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application service for managing Conversation aggregates.
 * This service delegates to the Conversation aggregate for business logic
 * while handling service-level concerns like validation, encryption, and event publishing.
 */
@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;
    private final EncryptionService encryptionService;
    private final RoomMembershipValidator roomValidator;
    private final E2EEKeyManagementService e2eeKeyManagementService;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            EventPublisher eventPublisher,
            EncryptionService encryptionService,
            RoomMembershipValidator roomValidator,
            E2EEKeyManagementService e2eeKeyManagementService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.roomValidator = roomValidator;
        this.e2eeKeyManagementService = e2eeKeyManagementService;
    }

    /**
     * Add a message to a room's conversation.
     * This method:
     * 1. Validates user membership in the room
     * 2. Encrypts the message content (E2EE or at-rest)
     * 3. Delegates to Conversation aggregate for limit enforcement
     * 4. Persists the message and updates the conversation
     * 5. Publishes a domain event
     *
     * @param roomId the room ID
     * @param senderId the sender's user ID
     * @param content the message content (plaintext for at-rest, ciphertext for E2EE)
     * @param now the current timestamp
     * @return the saved message
     */
    public Message addMessage(Long roomId, Long senderId, String content, Instant now) {
        // Validate user is a member of the room
        roomValidator.validateAndGetRoom(roomId, senderId);

        // Encrypt content based on room's encryption mode
        String ciphertext;
        if (e2eeKeyManagementService.isE2EEEnabled(roomId)) {
            // E2EE mode: content is already encrypted by client
            ciphertext = content;
        } else {
            // Legacy mode: encrypt at-rest with AES-GCM
            ciphertext = encryptionService.encrypt(content);
        }

        // Get or create conversation for this room
        Conversation conversation = conversationRepository.findByRoomId(roomId)
            .orElseGet(() -> new Conversation(roomId, now));

        // Create message
        Message message = new Message(senderId, roomId, new MessageContent(ciphertext), now);

        // Add message to conversation (enforces message limit)
        try {
            conversation.addMessage(message, now);
        } catch (InvalidMessageError e) {
            throw new InvalidMessageError("Cannot add message: " + e.getMessage());
        }

        // Save conversation (with updated timestamps)
        conversationRepository.save(conversation);

        // Set conversation ID on message and save
        message.setConversationId(conversation.getId());
        Message savedMessage = messageRepository.save(message);

        // Publish event
        eventPublisher.publish(MessageEvents.created(savedMessage));

        return savedMessage;
    }

    /**
     * Update a message in a room's conversation.
     * This method:
     * 1. Validates user membership in the room
     * 2. Validates message ownership
     * 3. Encrypts the updated content
     * 4. Delegates to Conversation aggregate for authorization
     * 5. Persists the changes and publishes an event
     *
     * @param roomId the room ID
     * @param userId the user ID attempting to edit
     * @param messageId the message ID to edit
     * @param content the new message content
     * @param now the current timestamp
     * @return the updated message
     */
    public Message updateMessage(Long roomId, Long userId, Long messageId, String content, Instant now) {
        // Validate user is a member of the room
        roomValidator.validateAndGetRoom(roomId, userId);

        // Get conversation
        Conversation conversation = conversationRepository.findByRoomId(roomId)
            .orElseThrow(() -> new InvalidMessageError("Conversation not found for this room"));

        // Validate authorization through aggregate
        if (!conversation.canEditMessage(messageId, userId)) {
            throw new ForbiddenError("Cannot edit message you didn't send");
        }

        // Encrypt content
        String ciphertext = encryptionService.encrypt(content);

        // Update message through aggregate
        conversation.updateMessage(messageId, new MessageContent(ciphertext), now);

        // Save conversation
        conversationRepository.save(conversation);

        // Get updated message
        Message updatedMessage = messageRepository.findById(messageId)
            .orElseThrow(() -> new InvalidMessageError("Message not found after update"));

        // Publish event
        eventPublisher.publish(MessageEvents.edited(updatedMessage));

        return updatedMessage;
    }

    /**
     * Delete a message from a room's conversation.
     * This method:
     * 1. Validates user membership in the room
     * 2. Validates message ownership
     * 3. Delegates to Conversation aggregate for authorization
     * 4. Deletes the message and publishes an event
     *
     * @param roomId the room ID
     * @param userId the user ID attempting to delete
     * @param messageId the message ID to delete
     * @param now the current timestamp
     */
    public void deleteMessage(Long roomId, Long userId, Long messageId, Instant now) {
        // Validate user is a member of the room
        roomValidator.validateAndGetRoom(roomId, userId);

        // Get conversation
        Conversation conversation = conversationRepository.findByRoomId(roomId)
            .orElseThrow(() -> new InvalidMessageError("Conversation not found for this room"));

        // Validate authorization through aggregate
        if (!conversation.canDeleteMessage(messageId, userId)) {
            throw new ForbiddenError("Cannot delete message you didn't send");
        }

        // Get message for event publishing
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new InvalidMessageError("Message not found"));

        // Delete message through aggregate
        conversation.deleteMessage(messageId, now);

        // Save conversation
        conversationRepository.save(conversation);

        // Delete message from repository
        messageRepository.deleteById(messageId);

        // Publish event
        eventPublisher.publish(MessageEvents.deleted(message));
    }

    /**
     * Get messages from a room's conversation with pagination.
     *
     * @param roomId the room ID
     * @param limit the maximum number of messages to return
     * @param offset the number of messages to skip
     * @return list of messages
     */
    public List<Message> getMessages(Long roomId, int limit, int offset) {
        return messageRepository.findByRoomId(roomId, limit, offset);
    }

    /**
     * Get messages from a room's conversation before a specific message (cursor-based pagination).
     *
     * @param roomId the room ID
     * @param beforeMessageId the message ID to get messages before
     * @param limit the maximum number of messages to return
     * @return list of messages
     */
    public List<Message> getMessagesBefore(Long roomId, Long beforeMessageId, int limit) {
        return messageRepository.findByRoomIdBefore(roomId, beforeMessageId, limit);
    }

    /**
     * Get the message count for a room's conversation.
     *
     * @param roomId the room ID
     * @return the number of messages in the conversation
     */
    public int getMessageCount(Long roomId) {
        return (int) messageRepository.countByRoomId(roomId);
    }
}
