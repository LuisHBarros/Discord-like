package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    private static final Long MESSAGE_ID = 1L;
    private static final Long SENDER_ID = 10L;
    private static final Long ROOM_ID = 20L;
    private static final String PLAINTEXT = "Hello, World!";
    private static final String CIPHERTEXT = "encrypted_content_here";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void constructor_createsMessageWithEncryptedContent() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);

        assertThat(message.getId()).isNull(); // ID is null until persisted
        assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
        assertThat(message.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(message.getContent().ciphertext()).isEqualTo(CIPHERTEXT);
        assertThat(message.getCreatedAt()).isEqualTo(NOW);
        assertThat(message.getEditedAt()).isNull();
    }

    @Test
    void reconstitute_restoresMessageFromDatabase() {
        Instant editedAt = NOW.plusSeconds(60);

        Message message = Message.reconstitute(
                MESSAGE_ID,
                SENDER_ID,
                ROOM_ID,
                new MessageContent(CIPHERTEXT),
                NOW,
                editedAt
        );

        assertThat(message.getId()).isEqualTo(MESSAGE_ID);
        assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
        assertThat(message.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(message.getContent().ciphertext()).isEqualTo(CIPHERTEXT);
        assertThat(message.getCreatedAt()).isEqualTo(NOW);
        assertThat(message.getEditedAt()).isEqualTo(editedAt);
    }

    @Test
    void edit_updatesContentAndTimestamps() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);
        Instant later = NOW.plusSeconds(60);
        String newCiphertext = "new_encrypted_content";

        message.edit(new MessageContent(newCiphertext), later);

        assertThat(message.getContent().ciphertext()).isEqualTo(newCiphertext);
        assertThat(message.getEditedAt()).isEqualTo(later);
    }

    @Test
    void edit_emptyContent_throwsException() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);
        Instant later = NOW.plusSeconds(60);

        assertThatThrownBy(() -> message.edit(new MessageContent(""), later))
                .isInstanceOf(InvalidMessageError.class)
                .hasMessageContaining("empty");
    }

    @Test
    void edit_nullContent_updatesContentToNull() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);
        Instant later = NOW.plusSeconds(60);

        // The edit method doesn't validate for null - it accepts it
        message.edit(null, later);

        assertThat(message.getContent()).isNull();
        assertThat(message.getEditedAt()).isEqualTo(later);
    }

    @Test
    void getSenderId_returnsCorrectSender() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);

        assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
    }

    @Test
    void getRoomId_returnsCorrectRoom() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);

        assertThat(message.getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void isEdited_whenNotEdited_returnsFalse() {
        Message message = new Message(SENDER_ID, ROOM_ID, new MessageContent(CIPHERTEXT), NOW);

        assertThat(message.isEdited()).isFalse();
    }

    @Test
    void isEdited_whenEdited_returnsTrue() {
        Message message = Message.reconstitute(
                MESSAGE_ID,
                SENDER_ID,
                ROOM_ID,
                new MessageContent(CIPHERTEXT),
                NOW,
                NOW.plusSeconds(60)
        );

        assertThat(message.isEdited()).isTrue();
    }
}