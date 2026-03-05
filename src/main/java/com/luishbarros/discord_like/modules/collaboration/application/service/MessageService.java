package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.collaboration.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;
    private final EncryptionService encryptionService;
    private final RoomMembershipValidator roomValidator;
    private final E2EEKeyManagementService e2eeKeyManagementService;

    public MessageService(
            MessageRepository messageRepository,
            EventPublisher eventPublisher,
            EncryptionService encryptionService,
            RoomMembershipValidator roomValidator,
            E2EEKeyManagementService e2eeKeyManagementService
    ) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.roomValidator = roomValidator;
        this.e2eeKeyManagementService = e2eeKeyManagementService;
    }

    public Message createMessage(Long senderId, Long roomId, String content, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);

        // Check if room uses E2EE
        String ciphertext;
        if (e2eeKeyManagementService.isE2EEEnabled(roomId)) {
            // E2EE mode: content is already encrypted by client
            ciphertext = content;
        } else {
            // Legacy mode: encrypt at-rest with AES-GCM
            ciphertext = encryptionService.encrypt(content);
        }

        Message message = new Message(senderId, roomId, new MessageContent(ciphertext), now);
        Message saved = messageRepository.save(message);
        eventPublisher.publish(MessageEvents.created(saved));
        return saved;
    }

    public List<Message> findByRoomId(Long roomId, Long userId, int limit, int offset) {
        roomValidator.validateAndGetRoom(roomId, userId);
        return messageRepository.findByRoomId(roomId, limit, offset);
    }

    public List<Message> findByRoomIdBefore(Long roomId, Long userId, Long beforeMessageId, int limit) {
        roomValidator.validateAndGetRoom(roomId, userId);
        return messageRepository.findByRoomIdBefore(roomId, beforeMessageId, limit);
    }

    public Message updateMessage(Long senderId, Long roomId, Long messageId, String plaintext, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);
        Message message = messageRepository
                .findByIdAndRoomId(messageId, roomId)
                .orElseThrow(() -> new InvalidMessageError("Message not found in this room"));
        validateOwnership(message, senderId);
        String ciphertext = encryptionService.encrypt(plaintext);
        message.edit(new MessageContent(ciphertext), now);
        messageRepository.save(message);
        eventPublisher.publish(MessageEvents.edited(message));
        return message;
    }

    public void deleteMessage(Long senderId, Long roomId, Long messageId) {
        roomValidator.validateAndGetRoom(roomId, senderId);
        Message message = messageRepository
                .findByIdAndRoomId(messageId, roomId)
                .orElseThrow(() -> new InvalidMessageError("Message not found in this room"));
        validateOwnership(message, senderId);
        messageRepository.deleteById(message.getId());
        eventPublisher.publish(MessageEvents.deleted(message));
    }

    private void validateOwnership(Message message, Long senderId) {
        if (!message.getSenderId().equals(senderId)) {
            throw new ForbiddenError("Cannot modify message you didn't send");
        }
    }
}