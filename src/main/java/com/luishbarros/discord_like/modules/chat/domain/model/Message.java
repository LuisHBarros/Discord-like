package com.luishbarros.discord_like.modules.chat.domain.model;

import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

public class Message extends BaseEntity {

    private Long senderId;
    private Long roomId;
    private String ciphertext;
    private Instant createdAt;
    private Instant editedAt;

    protected Message() {}

    public Message(Long senderId, Long roomId, String ciphertext, Instant createdAt) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
        this.senderId = senderId;
        this.roomId = roomId;
        this.ciphertext = ciphertext;
        this.createdAt = createdAt;
        this.editedAt = null;
    }

    public static Message reconstitute(Long id, Long senderId, Long roomId, String ciphertext, Instant createdAt, Instant editedAt) {
        Message message = new Message();
        message.id = id;
        message.senderId = senderId;
        message.roomId = roomId;
        message.ciphertext = ciphertext;
        message.createdAt = createdAt;
        message.editedAt = editedAt;
        return message;
    }

    public void edit(String newCiphertext, Instant editedAt) {
        if (newCiphertext == null || newCiphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
        this.ciphertext = newCiphertext;
        this.editedAt = editedAt;
    }

    public Long getSenderId()     { return senderId; }
    public Long getRoomId()       { return roomId; }
    public String getCiphertext() { return ciphertext; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt()  { return editedAt; }
    public boolean isEdited()     { return editedAt != null; }
}