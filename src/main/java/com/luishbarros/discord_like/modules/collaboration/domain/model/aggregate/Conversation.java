package com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.shared.domain.model.BaseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public class Conversation extends BaseEntity {

    private Long roomId;
    private final SortedSet<Message> messages = new TreeSet<>((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
    private Instant lastActivityAt;
    private Instant createdAt;
    private Instant updatedAt;
    private static final int MAX_MESSAGES = 10000;

    protected Conversation() {}

    public Conversation(Long roomId, Instant createdAt) {
        this.roomId = roomId;
        this.lastActivityAt = createdAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Conversation reconstitute(Long id, Long roomId, SortedSet<Message> messages, Instant lastActivityAt) {
        return reconstitute(id, roomId, messages, lastActivityAt, lastActivityAt, lastActivityAt);
    }

    public static Conversation reconstitute(Long id, Long roomId, SortedSet<Message> messages, Instant lastActivityAt, Instant createdAt, Instant updatedAt) {
        Conversation conversation = new Conversation();
        conversation.id = id;
        conversation.roomId = roomId;
        conversation.messages.addAll(messages);
        conversation.lastActivityAt = lastActivityAt;
        conversation.createdAt = createdAt;
        conversation.updatedAt = updatedAt;
        return conversation;
    }

    public void addMessage(Message message, Instant now) {
        if (messages.size() >= MAX_MESSAGES) {
            throw InvalidMessageError.messageLimitExceeded("Conversation has reached maximum message limit");
        }
        messages.add(message);
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public void updateMessage(Long messageId, MessageContent newContent, Instant now) {
        Message message = findMessage(messageId)
            .orElseThrow(() -> new RoomNotFoundError("Message not found"));
        message.edit(newContent, now);
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public void deleteMessage(Long messageId, Instant now) {
        Message message = findMessage(messageId)
            .orElseThrow(() -> new RoomNotFoundError("Message not found"));
        messages.remove(message);
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public List<Message> getMessages(int limit, int offset) {
        return messages.stream()
            .skip(offset)
            .limit(limit)
            .toList();
    }

    public List<Message> getMessagesBefore(Long messageId, int limit) {
        return messages.stream()
            .takeWhile(m -> m.getId().compareTo(messageId) < 0)
            .toList()
            .reversed()
            .stream()
            .limit(limit)
            .toList();
    }

    public boolean canEditMessage(Long messageId, Long userId) {
        return findMessage(messageId)
            .map(m -> m.getSenderId().equals(userId))
            .orElse(false);
    }

    public boolean canDeleteMessage(Long messageId, Long userId) {
        return findMessage(messageId)
            .map(m -> m.getSenderId().equals(userId))
            .orElse(false);
    }

    private Optional<Message> findMessage(Long messageId) {
        return messages.stream()
            .filter(m -> m.getId().equals(messageId))
            .findFirst();
    }

    // Getters
    public Long getRoomId() { return roomId; }
    public SortedSet<Message> getMessages() { return java.util.Collections.unmodifiableSortedSet(messages); }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public int getMessageCount() { return messages.size(); }

    // Timestamp getters (inherited from BaseEntity, but provided here for clarity)
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
