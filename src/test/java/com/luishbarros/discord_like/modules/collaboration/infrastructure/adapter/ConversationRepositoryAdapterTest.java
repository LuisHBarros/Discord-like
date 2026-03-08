package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Message;
import com.luishbarros.discord_like.modules.collaboration.domain.model.aggregate.Conversation;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.MessageContent;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.ConversationJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.MessageJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.ConversationJpaRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.MessageJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationRepositoryAdapterTest {

    private static final Long    ROOM_ID         = 10L;
    private static final Long    CONVERSATION_ID = 100L;
    private static final Long    SENDER_ID       = 1L;
    private static final Long    MESSAGE_ID      = 200L;
    private static final Instant NOW             = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER           = Instant.parse("2026-01-01T01:00:00Z");

    @Mock
    private ConversationJpaRepository conversationJpaRepository;

    @Mock
    private MessageJpaRepository messageJpaRepository;

    @InjectMocks
    private ConversationRepositoryAdapter adapter;

    @Nested
    class FindByRoomId {

        @Test
        void givenExistingConversation_returnsDomainConversation() {
            ConversationJpaEntity jpaEntity = new ConversationJpaEntity(
                CONVERSATION_ID,
                ROOM_ID,
                LATER,
                NOW,
                LATER
            );

            MessageJpaEntity messageEntity = new MessageJpaEntity(
                MESSAGE_ID,
                SENDER_ID,
                ROOM_ID,
                CONVERSATION_ID,
                "encrypted content",
                NOW,
                null
            );

            when(conversationJpaRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(jpaEntity));
            when(messageJpaRepository.findByRoomId(ROOM_ID)).thenReturn(List.of(messageEntity));

            Optional<Conversation> result = adapter.findByRoomId(ROOM_ID);

            assertThat(result).isPresent();
            Conversation conversation = result.get();
            assertThat(conversation.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(conversation.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(conversation.getLastActivityAt()).isEqualTo(LATER);
            assertThat(conversation.getMessageCount()).isEqualTo(1);
        }

        @Test
        void givenNoConversation_returnsEmpty() {
            when(conversationJpaRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());

            Optional<Conversation> result = adapter.findByRoomId(ROOM_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Save {

        @Test
        void givenNewConversation_savesAndReturnsWithGeneratedId() {
            Conversation newConversation = new Conversation(ROOM_ID, NOW);

            ConversationJpaEntity savedEntity = new ConversationJpaEntity(
                CONVERSATION_ID,
                ROOM_ID,
                NOW,
                NOW,
                NOW
            );

            when(conversationJpaRepository.save(any(ConversationJpaEntity.class))).thenReturn(savedEntity);

            Conversation savedConversation = adapter.save(newConversation);

            assertThat(savedConversation.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(savedConversation.getRoomId()).isEqualTo(ROOM_ID);

            ArgumentCaptor<ConversationJpaEntity> captor = ArgumentCaptor.forClass(ConversationJpaEntity.class);
            verify(conversationJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isNull(); // New conversation has null ID before save
        }

        @Test
        void givenExistingConversation_updatesAndReturns() {
            Conversation existingConversation = Conversation.reconstitute(
                CONVERSATION_ID,
                ROOM_ID,
                new java.util.TreeSet<>(),
                LATER
            );

            ConversationJpaEntity updatedEntity = new ConversationJpaEntity(
                CONVERSATION_ID,
                ROOM_ID,
                LATER,
                NOW,
                LATER
            );

            when(conversationJpaRepository.save(any(ConversationJpaEntity.class))).thenReturn(updatedEntity);
            when(messageJpaRepository.findByRoomId(ROOM_ID)).thenReturn(List.of());

            Conversation savedConversation = adapter.save(existingConversation);

            assertThat(savedConversation.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(savedConversation.getRoomId()).isEqualTo(ROOM_ID);
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingConversation_returnsDomainConversation() {
            ConversationJpaEntity jpaEntity = new ConversationJpaEntity(
                CONVERSATION_ID,
                ROOM_ID,
                LATER,
                NOW,
                LATER
            );

            when(conversationJpaRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(jpaEntity));
            when(messageJpaRepository.findByRoomId(ROOM_ID)).thenReturn(List.of());

            Optional<Conversation> result = adapter.findById(CONVERSATION_ID);

            assertThat(result).isPresent();
            Conversation conversation = result.get();
            assertThat(conversation.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(conversation.getRoomId()).isEqualTo(ROOM_ID);
        }

        @Test
        void givenNoConversation_returnsEmpty() {
            when(conversationJpaRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

            Optional<Conversation> result = adapter.findById(CONVERSATION_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteById_delegatesToJpaRepository() {
            adapter.deleteById(CONVERSATION_ID);

            verify(conversationJpaRepository).deleteById(CONVERSATION_ID);
        }

        @Test
        void deleteByRoomId_delegatesToJpaRepository() {
            adapter.deleteByRoomId(ROOM_ID);

            verify(conversationJpaRepository).deleteByRoomId(ROOM_ID);
        }
    }
}
