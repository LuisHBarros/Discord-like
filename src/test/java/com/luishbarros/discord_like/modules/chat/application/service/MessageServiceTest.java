package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.chat.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Message;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private EncryptionService encryptionService;
    @Mock private RoomMembershipValidator roomValidator;

    @InjectMocks
    private MessageService messageService;

    private static final Long    ROOM_ID    = 10L;
    private static final Long    SENDER_ID  = 1L;
    private static final Long    OTHER_ID   = 2L;
    private static final Long    MESSAGE_ID = 100L;
    private static final Instant NOW        = Instant.parse("2026-01-01T00:00:00Z");
    private static final String  PLAINTEXT  = "Hello world";
    private static final String  CIPHERTEXT = "enc:Hello world";

    private Room room() {
        return Room.reconstitute(ROOM_ID, "General", SENDER_ID, Set.of(SENDER_ID, OTHER_ID), NOW, NOW);
    }

    private Message message(Long senderId) {
        return Message.reconstitute(MESSAGE_ID, senderId, ROOM_ID, CIPHERTEXT, NOW, null);
    }

    // ─── CreateMessage ────────────────────────────────────────────────────────

    @Nested
    class CreateMessage {

        @Test
        void givenMember_encryptsAndSavesAndPublishesEvent() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(encryptionService.encrypt(PLAINTEXT)).thenReturn(CIPHERTEXT);
            when(messageRepository.save(any(Message.class))).thenReturn(message(SENDER_ID));

            Message result = messageService.createMessage(SENDER_ID, ROOM_ID, PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            assertThat(result.getCiphertext()).isEqualTo(CIPHERTEXT);
            assertThat(result.getSenderId()).isEqualTo(SENDER_ID);
            verify(messageRepository).save(any(Message.class));

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.CREATED);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID))
                    .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> messageService.createMessage(OTHER_ID, ROOM_ID, PLAINTEXT, NOW))
                    .isInstanceOf(UserNotInRoomError.class);

            verify(messageRepository, never()).save(any());
        }
    }

    // ─── FindByRoomId ─────────────────────────────────────────────────────────

    @Nested
    class FindByRoomId {

        @Test
        void givenMember_returnsPaginatedMessages() {
            List<Message> page = List.of(message(SENDER_ID));
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByRoomId(ROOM_ID, 20, 0)).thenReturn(page);

            List<Message> result = messageService.findByRoomId(ROOM_ID, SENDER_ID, 20, 0);

            assertThat(result).isEqualTo(page);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID))
                    .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> messageService.findByRoomId(ROOM_ID, OTHER_ID, 20, 0))
                    .isInstanceOf(UserNotInRoomError.class);
        }
    }

    // ─── FindByRoomIdBefore ───────────────────────────────────────────────────

    @Nested
    class FindByRoomIdBefore {

        @Test
        void givenMember_returnsCursorMessages() {
            List<Message> messages = List.of(message(SENDER_ID));
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByRoomIdBefore(ROOM_ID, MESSAGE_ID, 20)).thenReturn(messages);

            List<Message> result = messageService.findByRoomIdBefore(ROOM_ID, SENDER_ID, MESSAGE_ID, 20);

            assertThat(result).isEqualTo(messages);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID))
                    .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> messageService.findByRoomIdBefore(ROOM_ID, OTHER_ID, MESSAGE_ID, 20))
                    .isInstanceOf(UserNotInRoomError.class);
        }
    }

    // ─── UpdateMessage ────────────────────────────────────────────────────────

    @Nested
    class UpdateMessage {

        private static final String NEW_PLAINTEXT  = "Edited text";
        private static final String NEW_CIPHERTEXT = "enc:Edited text";

        @Test
        void givenSender_encryptsAndEditsAndPublishesEvent() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID))
                    .thenReturn(Optional.of(message(SENDER_ID)));
            when(encryptionService.encrypt(NEW_PLAINTEXT)).thenReturn(NEW_CIPHERTEXT);

            Message result = messageService.updateMessage(SENDER_ID, ROOM_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW);

            assertThat(result.getCiphertext()).isEqualTo(NEW_CIPHERTEXT);
            assertThat(result.isEdited()).isTrue();
            verify(messageRepository).save(result);

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.EDITED);
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID))
                    .thenReturn(Optional.of(message(SENDER_ID)));

            assertThatThrownBy(() -> messageService.updateMessage(OTHER_ID, ROOM_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW))
                    .isInstanceOf(ForbiddenError.class);

            verify(messageRepository, never()).save(any());
        }

        @Test
        void givenMessageNotFound_throwsInvalidMessageError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.updateMessage(SENDER_ID, ROOM_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW))
                    .isInstanceOf(InvalidMessageError.class);
        }
    }

    // ─── DeleteMessage ────────────────────────────────────────────────────────

    @Nested
    class DeleteMessage {

        @Test
        void givenSender_deletesAndPublishesEvent() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID))
                    .thenReturn(Optional.of(message(SENDER_ID)));

            messageService.deleteMessage(SENDER_ID, ROOM_ID, MESSAGE_ID);

            verify(messageRepository).deleteById(MESSAGE_ID);

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.DELETED);
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID))
                    .thenReturn(Optional.of(message(SENDER_ID)));

            assertThatThrownBy(() -> messageService.deleteMessage(OTHER_ID, ROOM_ID, MESSAGE_ID))
                    .isInstanceOf(ForbiddenError.class);

            verify(messageRepository, never()).deleteById(any());
        }

        @Test
        void givenMessageNotFound_throwsInvalidMessageError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(messageRepository.findByIdAndRoomId(MESSAGE_ID, ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.deleteMessage(SENDER_ID, ROOM_ID, MESSAGE_ID))
                    .isInstanceOf(InvalidMessageError.class);
        }
    }
}
