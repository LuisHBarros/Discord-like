package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;

public class Message extends BaseEntity {

    private Long senderId;
    private Long roomId;
    private Long conversationId;
    private MessageContent content;
    private Instant createdAt;
    private Instant editedAt;

    protected Message() {}

    public Message(Long senderId, Long roomId, MessageContent content, Instant createdAt) {
        this.senderId = senderId;
        this.roomId = roomId;
        this.content = content;
        this.createdAt = createdAt;
        this.editedAt = null;
    }

    public static Message reconstitute(Long id, Long senderId, Long roomId, MessageContent content, Instant createdAt, Instant editedAt) {
        Message message = new Message();
        message.id = id;
        message.senderId = senderId;
        message.roomId = roomId;
        message.content = content;
        message.createdAt = createdAt;
        message.editedAt = editedAt;
        return message;
    }

    public static Message reconstitute(Long id, Long senderId, Long roomId, Long conversationId, MessageContent content, Instant createdAt, Instant editedAt) {
        Message message = new Message();
        message.id = id;
        message.senderId = senderId;
        message.roomId = roomId;
        message.conversationId = conversationId;
        message.content = content;
        message.createdAt = createdAt;
        message.editedAt = editedAt;
        return message;
    }

    public void edit(MessageContent newContent, Instant editedAt) {
        this.content = newContent;
        this.editedAt = editedAt;
    }

    public Long getSenderId()       { return senderId; }
    public Long getRoomId()         { return roomId; }
    public Long getConversationId() { return conversationId; }
    public MessageContent getContent() { return content; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getEditedAt()   { return editedAt; }
    public boolean isEdited()      { return editedAt != null; }

    // Setter for conversation ID (used by repository/application services)
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}
