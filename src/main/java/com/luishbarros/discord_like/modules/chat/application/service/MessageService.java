package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import com.luishbarros.discord_like.modules.chat.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
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

    public MessageService(
            MessageRepository messageRepository,
            EventPublisher eventPublisher,
            EncryptionService encryptionService,
            RoomMembershipValidator roomValidator
    ) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.roomValidator = roomValidator;
    }

    public Message createMessage(Long senderId, Long roomId, String plaintext, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);
        String ciphertext = encryptionService.encrypt(plaintext);
        Message message = new Message(senderId, roomId, ciphertext, now);
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
        message.edit(ciphertext, now);
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