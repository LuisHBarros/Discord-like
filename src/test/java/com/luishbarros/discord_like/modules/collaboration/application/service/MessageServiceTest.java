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
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.MessageRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private ConversationService conversationService;
    @Mock private MessageRepository messageRepository;
    @Mock private RoomMembershipValidator roomValidator;

    @InjectMocks
    private MessageService messageService;

    private static final Long    ROOM_ID    = 10L;
    private static final Long    SENDER_ID  = 1L;
    private static final Long    OTHER_ID   = 2L;
    private static final Long    MESSAGE_ID = 100L;
    private static final Instant NOW        = Instant.parse("2026-01-01T00:00:00Z");
    private static final String  PLAINTEXT  = "Hello world";
    private static final MessageContent CIPHERTEXT = new MessageContent("enc:Hello world");

    private Room room() {
        return Room.reconstitute(ROOM_ID, new RoomName("General"), SENDER_ID, Set.of(SENDER_ID, OTHER_ID), NOW, NOW);
    }

    private Message message(Long senderId) {
        return Message.reconstitute(MESSAGE_ID, senderId, ROOM_ID, 100L, CIPHERTEXT, NOW, null);
    }

    // ─── CreateMessage ────────────────────────────────────────────────────────

    @Nested
    class CreateMessage {

        @Test
        void givenMember_delegatesToConversationService() {
            when(conversationService.addMessage(ROOM_ID, SENDER_ID, PLAINTEXT, NOW)).thenReturn(message(SENDER_ID));

            Message result = messageService.createMessage(SENDER_ID, ROOM_ID, PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            assertThat(result.getContent()).isEqualTo(CIPHERTEXT);
            assertThat(result.getSenderId()).isEqualTo(SENDER_ID);
            verify(conversationService).addMessage(ROOM_ID, SENDER_ID, PLAINTEXT, NOW);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(conversationService.addMessage(ROOM_ID, OTHER_ID, PLAINTEXT, NOW))
                    .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> messageService.createMessage(OTHER_ID, ROOM_ID, PLAINTEXT, NOW))
                    .isInstanceOf(UserNotInRoomError.class);
        }
    }

    // ─── FindByRoomId ─────────────────────────────────────────────────────────

    @Nested
    class FindByRoomId {

        @Test
        void givenMember_returnsPaginatedMessages() {
            List<Message> page = List.of(message(SENDER_ID));
            when(conversationService.getMessages(ROOM_ID, 20, 0)).thenReturn(page);

            List<Message> result = messageService.findByRoomId(ROOM_ID, SENDER_ID, 20, 0);

            assertThat(result).isEqualTo(page);
            verify(conversationService).getMessages(ROOM_ID, 20, 0);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(conversationService.getMessages(ROOM_ID, 20, 0))
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
            when(conversationService.getMessagesBefore(ROOM_ID, MESSAGE_ID, 20)).thenReturn(messages);

            List<Message> result = messageService.findByRoomIdBefore(ROOM_ID, SENDER_ID, MESSAGE_ID, 20);

            assertThat(result).isEqualTo(messages);
            verify(conversationService).getMessagesBefore(ROOM_ID, MESSAGE_ID, 20);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(conversationService.getMessagesBefore(ROOM_ID, MESSAGE_ID, 20))
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
        void givenSender_delegatesToConversationService() {
            when(conversationService.updateMessage(ROOM_ID, SENDER_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW))
                    .thenReturn(message(SENDER_ID));

            Message result = messageService.updateMessage(SENDER_ID, ROOM_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW);

            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            verify(conversationService).updateMessage(ROOM_ID, SENDER_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW);
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            when(conversationService.updateMessage(ROOM_ID, OTHER_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW))
                    .thenThrow(new ForbiddenError("Cannot edit message you didn't send"));

            assertThatThrownBy(() -> messageService.updateMessage(OTHER_ID, ROOM_ID, MESSAGE_ID, NEW_PLAINTEXT, NOW))
                    .isInstanceOf(ForbiddenError.class);
        }
    }

    // ─── DeleteMessage ────────────────────────────────────────────────────────

    @Nested
    class DeleteMessage {

        @Test
        void givenSender_delegatesToConversationService() {
            doNothing().when(conversationService).deleteMessage(eq(ROOM_ID), eq(SENDER_ID), eq(MESSAGE_ID), any(Instant.class));

            messageService.deleteMessage(SENDER_ID, ROOM_ID, MESSAGE_ID);

            verify(conversationService).deleteMessage(eq(ROOM_ID), eq(SENDER_ID), eq(MESSAGE_ID), any(Instant.class));
        }

        @Test
        void givenNonSender_throwsForbiddenError() {
            doThrow(new ForbiddenError("Cannot delete message you didn't send"))
                    .when(conversationService).deleteMessage(eq(ROOM_ID), eq(OTHER_ID), eq(MESSAGE_ID), any(Instant.class));

            assertThatThrownBy(() -> messageService.deleteMessage(OTHER_ID, ROOM_ID, MESSAGE_ID))
                    .isInstanceOf(ForbiddenError.class);
        }
    }
}
