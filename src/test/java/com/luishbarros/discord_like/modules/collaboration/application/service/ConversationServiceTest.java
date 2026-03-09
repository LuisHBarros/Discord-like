package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidMessageError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.collaboration.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.RoomName;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.EncryptionService;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.ConversationRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
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
class ConversationServiceTest {

    private static final Long    ROOM_ID         = 10L;
    private static final Long    CONVERSATION_ID = 100L;
    private static final Long    SENDER_ID       = 1L;
    private static final Long    OTHER_ID        = 2L;
    private static final Long    MESSAGE_ID      = 200L;
    private static final Instant NOW             = Instant.parse("2026-01-01T00:00:00Z");
    private static final String  PLAINTEXT       = "Hello world";
    private static final String  CIPHERTEXT      = "enc:Hello world";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private RoomMembershipValidator roomValidator;

    @Mock
    private E2EEKeyManagementService e2eeKeyManagementService;

    @InjectMocks
    private ConversationService conversationService;

    private Room room() {
        return Room.reconstitute(ROOM_ID, new RoomName("General"), SENDER_ID, NOW, NOW);
    }

    private Message message(Long senderId) {
        return Message.reconstitute(MESSAGE_ID, senderId, ROOM_ID, CONVERSATION_ID, new MessageContent(CIPHERTEXT), NOW, null);
    }

    // ─── AddMessage ─────────────────────────────────────────────────────────────

    @Nested
    class AddMessage {

        @Test
        void givenMemberAndNewConversation_createsConversationAndSavesMessage() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(e2eeKeyManagementService.isE2EEEnabled(ROOM_ID)).thenReturn(false);
            when(encryptionService.encrypt(PLAINTEXT)).thenReturn(CIPHERTEXT);
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation c = invocation.getArgument(0);
                return Conversation.reconstitute(CONVERSATION_ID, c.getRoomId(), c.getMessages(), c.getLastActivityAt());
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message m = invocation.getArgument(0);
                m.setConversationId(CONVERSATION_ID);
                return Message.reconstitute(MESSAGE_ID, m.getSenderId(), m.getRoomId(), CONVERSATION_ID, m.getContent(), m.getCreatedAt(), m.getEditedAt());
            });

            Message result = conversationService.addMessage(ROOM_ID, SENDER_ID, PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            assertThat(result.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(result.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(result.getConversationId()).isEqualTo(CONVERSATION_ID);
            verify(conversationRepository).save(any(Conversation.class));
            verify(messageRepository).save(any(Message.class));

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.CREATED);
        }

        @Test
        void givenMemberAndExistingConversation_addsToExistingConversation() {
            Conversation existingConversation = new Conversation(ROOM_ID, NOW.minusSeconds(3600));
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(e2eeKeyManagementService.isE2EEEnabled(ROOM_ID)).thenReturn(false);
            when(encryptionService.encrypt(PLAINTEXT)).thenReturn(CIPHERTEXT);
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(existingConversation);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message m = invocation.getArgument(0);
                return message(SENDER_ID);
            });

            Message result = conversationService.addMessage(ROOM_ID, SENDER_ID, PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            verify(conversationRepository).save(any(Conversation.class));
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID))
                .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> conversationService.addMessage(ROOM_ID, OTHER_ID, PLAINTEXT, NOW))
                .isInstanceOf(UserNotInRoomError.class);

            verify(conversationRepository, never()).save(any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        void givenE2EEEnabledRoom_doesNotEncryptContent() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(e2eeKeyManagementService.isE2EEEnabled(ROOM_ID)).thenReturn(true);
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation c = invocation.getArgument(0);
                return Conversation.reconstitute(CONVERSATION_ID, c.getRoomId(), c.getMessages(), c.getLastActivityAt());
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message m = invocation.getArgument(0);
                // Return the message with the same content that was passed in
                return Message.reconstitute(MESSAGE_ID, m.getSenderId(), m.getRoomId(), CONVERSATION_ID, m.getContent(), m.getCreatedAt(), m.getEditedAt());
            });

            // In E2EE mode, the content is already encrypted by client
            String alreadyEncryptedContent = "client:encrypted:content";
            Message result = conversationService.addMessage(ROOM_ID, SENDER_ID, alreadyEncryptedContent, NOW);

            // Verify encryptionService was not called
            verify(encryptionService, never()).encrypt(any());
            assertThat(result.getContent().ciphertext()).isEqualTo(alreadyEncryptedContent);
        }
    }

    // ─── UpdateMessage ───────────────────────────────────────────────────────────

    @Nested
    class UpdateMessage {

        @Test
        void givenSender_updatesMessageAndPublishesEvent() {
            Conversation conversation = Conversation.reconstitute(
                CONVERSATION_ID,
                ROOM_ID,
                new java.util.TreeSet<>(),
                NOW
            );
            conversation.addMessage(message(SENDER_ID), NOW);

            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(conversation));
            when(encryptionService.encrypt(PLAINTEXT)).thenReturn(CIPHERTEXT);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message(SENDER_ID)));

            Message result = conversationService.updateMessage(ROOM_ID, SENDER_ID, MESSAGE_ID, PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            verify(messageRepository).findById(MESSAGE_ID);

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.EDITED);
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            Conversation conversation = Conversation.reconstitute(
                CONVERSATION_ID,
                ROOM_ID,
                new java.util.TreeSet<>(),
                NOW
            );
            conversation.addMessage(message(SENDER_ID), NOW);

            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID)).thenReturn(room());
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> conversationService.updateMessage(ROOM_ID, OTHER_ID, MESSAGE_ID, PLAINTEXT, NOW))
                .isInstanceOf(ForbiddenError.class);

            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void givenConversationNotFound_throwsInvalidMessageError() {
            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.updateMessage(ROOM_ID, SENDER_ID, MESSAGE_ID, PLAINTEXT, NOW))
                .isInstanceOf(InvalidMessageError.class)
                .hasMessageContaining("Conversation not found");
        }
    }

    // ─── DeleteMessage ───────────────────────────────────────────────────────────

    @Nested
    class DeleteMessage {

        @Test
        void givenSender_deletesMessageAndPublishesEvent() {
            Conversation conversation = Conversation.reconstitute(
                CONVERSATION_ID,
                ROOM_ID,
                new java.util.TreeSet<>(),
                NOW
            );
            conversation.addMessage(message(SENDER_ID), NOW);

            when(roomValidator.validateAndGetRoom(ROOM_ID, SENDER_ID)).thenReturn(room());
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(conversation));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message(SENDER_ID)));

            conversationService.deleteMessage(ROOM_ID, SENDER_ID, MESSAGE_ID, NOW);

            verify(messageRepository).findById(MESSAGE_ID);
            verify(messageRepository).deleteById(MESSAGE_ID);

            ArgumentCaptor<MessageEvents> captor = ArgumentCaptor.forClass(MessageEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(MessageEvents.EventType.DELETED);
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            Conversation conversation = Conversation.reconstitute(
                CONVERSATION_ID,
                ROOM_ID,
                new java.util.TreeSet<>(),
                NOW
            );
            conversation.addMessage(message(SENDER_ID), NOW);

            when(roomValidator.validateAndGetRoom(ROOM_ID, OTHER_ID)).thenReturn(room());
            when(conversationRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> conversationService.deleteMessage(ROOM_ID, OTHER_ID, MESSAGE_ID, NOW))
                .isInstanceOf(ForbiddenError.class);

            verify(messageRepository, never()).deleteById(any());
        }
    }

    // ─── GetMessages ──────────────────────────────────────────────────────────────

    @Nested
    class GetMessages {

        @Test
        void returnsMessagesFromRepository() {
            List<Message> messages = List.of(message(SENDER_ID));
            when(messageRepository.findByRoomId(ROOM_ID, 20, 0)).thenReturn(messages);

            List<Message> result = conversationService.getMessages(ROOM_ID, 20, 0);

            assertThat(result).isEqualTo(messages);
        }

        @Test
        void returnsMessagesBeforeCursor() {
            List<Message> messages = List.of(message(SENDER_ID));
            when(messageRepository.findByRoomIdBefore(ROOM_ID, MESSAGE_ID, 20)).thenReturn(messages);

            List<Message> result = conversationService.getMessagesBefore(ROOM_ID, MESSAGE_ID, 20);

            assertThat(result).isEqualTo(messages);
        }

        @Test
        void getMessageCount_returnsCountFromRepository() {
            when(messageRepository.countByRoomId(ROOM_ID)).thenReturn(42L);

            int result = conversationService.getMessageCount(ROOM_ID);

            assertThat(result).isEqualTo(42);
        }
    }
}
