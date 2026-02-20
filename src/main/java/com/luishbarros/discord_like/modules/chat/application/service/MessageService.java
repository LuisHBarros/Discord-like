package com.luishbarros.discord_like.modules.chat.application.service;


import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import com.luishbarros.discord_like.modules.chat.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.chat.domain.ports.EventPublisher;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;
    private final EncryptionService encryptionService;
    private final RoomMembershipValidator roomValidator;


    public MessageService(MessageRepository messageRepository, EventPublisher eventPublisher, EncryptionService encryptionService, RoomMembershipValidator roomValidator) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.roomValidator = roomValidator;
    }

    public Message createMessage(UUID senderId, UUID roomId, String plaintext, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);

        String ciphertext = encryptionService.encrypt(plaintext);
        Message message = new Message(senderId, roomId, ciphertext, now);
        messageRepository.save(message);
        eventPublisher.publish(MessageEvents.created(message));

        return message;
    }

    public Message updateMessage(UUID senderId, UUID roomId, UUID messageId, String plaintext, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);

        Message message = messageRepository
                .findByIdAndRoomId(messageId, roomId)
                .orElseThrow(() -> new InvalidMessageError("Message not found in this room"));

        validateOwnership(message, senderId);

        String ciphertext = encryptionService.encrypt(plaintext);
        message.edit(ciphertext, now);

        messageRepository.save(message);
        eventPublisher.publish(MessageEvents.edited(message));

        return message;
    }

    public void deleteMessage(UUID senderId, UUID roomId, UUID messageId) {
        roomValidator.validateAndGetRoom(roomId, senderId);
        Message message = messageRepository
                .findByIdAndRoomId(messageId, roomId)
                .orElseThrow(() -> new InvalidMessageError("Message not found in this room"));

        validateOwnership(message, senderId);

        messageRepository.deleteById(message.getId());
        eventPublisher.publish(MessageEvents.deleted(message));
    }

    private void validateOwnership(Message message, UUID senderId) {
        if (!message.getSenderId().equals(senderId)) {
            throw new ForbiddenError("Cannot modify message you didn't send");
        }
    }
}