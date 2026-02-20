package com.luishbarros.discord_like.modules.chat.domain.model;

import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;

import java.time.Instant;
import java.util.UUID;

public class Message {
    private final UUID id;
    private final UUID senderId;
    private final UUID roomId;
    private String ciphertext;
    private final Instant createdAt;
    private Instant editedAt;

    public Message(UUID senderId, UUID roomId, String ciphertext, Instant createdAt) {
        if(ciphertext == null || ciphertext.isBlank()){
            throw InvalidMessageError.emptyContent();
        }
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.roomId = roomId;
        this.ciphertext = ciphertext;
        this.createdAt = createdAt;
        this.editedAt = null;
    }

    private Message(UUID id, UUID senderId, UUID roomId, String ciphertext, Instant createdAt, Instant editedAt) {
        this.id = id;
        this.senderId = senderId;
        this.roomId = roomId;
        this.ciphertext = ciphertext;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
    }

    public static Message reconstitute(UUID id, UUID senderId, UUID roomId, String ciphertext, Instant createdAt, Instant editedAt) {
        return new Message(id, senderId, roomId, ciphertext, createdAt, editedAt);
    }

    public UUID getId(){
        return this.id;
    }
    public UUID getSenderId() {
        return this.senderId;
    }

    public UUID getRoomId() {
        return this.roomId;
    }

    public String getCiphertext() {
        return this.ciphertext;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getEditedAt() {
        return this.editedAt;
    }

    public boolean isEdited(){
        return editedAt != null;
    }
    public void edit(String newCiphertext, Instant editedAt) {
        if (newCiphertext == null || newCiphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
        this.ciphertext = newCiphertext;
        this.editedAt = editedAt;
    }


}
