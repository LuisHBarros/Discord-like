package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.application.factory.InviteFactory;
import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidInviteCodeError;
import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.chat.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Invite;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.InviteRepository;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
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
class InviteServiceTest {

    @Mock private InviteFactory inviteFactory;
    @Mock private InviteRepository inviteRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomMembershipValidator membershipValidator;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private InviteService inviteService;

    private static final Long ROOM_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long JOINER_ID = 3L;
    private static final Long INVITE_ID = 50L;
    private static final String CODE_VALUE = "ABCD1234";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private Room roomWith2Members() {
        return Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID, MEMBER_ID), NOW, NOW);
    }

    private Invite unsavedInvite() {
        InviteCode code = new InviteCode(CODE_VALUE, OWNER_ID);
        return new Invite(ROOM_ID, OWNER_ID, code, NOW);
    }

    private Invite validInvite() {
        InviteCode code = new InviteCode(CODE_VALUE, OWNER_ID);
        return Invite.reconstitute(INVITE_ID, ROOM_ID, OWNER_ID, code, NOW, NOW.plusSeconds(3600 * 24));
    }

    private Invite expiredInvite() {
        InviteCode code = new InviteCode(CODE_VALUE, OWNER_ID);
        return Invite.reconstitute(INVITE_ID, ROOM_ID, OWNER_ID, code, NOW.minusSeconds(3600 * 48), NOW.minusSeconds(1));
    }

    @Nested
    class GenerateInvite {

        @Test
        void givenMember_savesInviteAndPublishesEvent() {
            Invite createdInvite = unsavedInvite();
            Invite savedInvite = validInvite();
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());
            when(inviteFactory.create(ROOM_ID, OWNER_ID, NOW)).thenReturn(createdInvite);
            when(inviteRepository.save(createdInvite)).thenReturn(savedInvite);

            Invite result = inviteService.generateInvite(ROOM_ID, OWNER_ID, NOW);

            assertThat(result).isSameAs(savedInvite);
            assertThat(result.getId()).isEqualTo(INVITE_ID);
            verify(inviteRepository).save(createdInvite);

            ArgumentCaptor<InviteEvents> captor = ArgumentCaptor.forClass(InviteEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(InviteEvents.EventType.CREATED);
            assertThat(captor.getValue().inviteId()).isEqualTo(INVITE_ID);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, JOINER_ID))
                    .thenThrow(new UserNotInRoomError(JOINER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> inviteService.generateInvite(ROOM_ID, JOINER_ID, NOW))
                    .isInstanceOf(UserNotInRoomError.class);

            verify(inviteRepository, never()).save(any());
        }
    }

    @Nested
    class AcceptInvite {

        @Test
        void givenValidCode_addsMemberAndPublishesBothEvents() {
            when(inviteRepository.findByCode(CODE_VALUE)).thenReturn(Optional.of(validInvite()));
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomWith2Members()));

            inviteService.acceptInvite(CODE_VALUE, JOINER_ID, NOW);

            ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getMemberIds()).contains(JOINER_ID);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, org.mockito.Mockito.times(2)).publish(eventCaptor.capture());
            List<Object> events = eventCaptor.getAllValues();
            assertThat(events).anyMatch(e -> e instanceof RoomEvents re
                    && re.type() == RoomEvents.EventType.MEMBER_JOINED);
            assertThat(events).anyMatch(e -> e instanceof InviteEvents ie
                    && ie.type() == InviteEvents.EventType.ACCEPTED);
        }

        @Test
        void givenUnknownCode_throwsInvalidInviteCodeError() {
            when(inviteRepository.findByCode(CODE_VALUE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inviteService.acceptInvite(CODE_VALUE, JOINER_ID, NOW))
                    .isInstanceOf(InvalidInviteCodeError.class);

            verify(roomRepository, never()).save(any());
        }

        @Test
        void givenExpiredInvite_throwsInvalidInviteCodeError() {
            when(inviteRepository.findByCode(CODE_VALUE)).thenReturn(Optional.of(expiredInvite()));

            assertThatThrownBy(() -> inviteService.acceptInvite(CODE_VALUE, JOINER_ID, NOW))
                    .isInstanceOf(InvalidInviteCodeError.class);

            verify(roomRepository, never()).save(any());
        }

        @Test
        void givenValidCodeButRoomDeleted_throwsRoomNotFoundError() {
            when(inviteRepository.findByCode(CODE_VALUE)).thenReturn(Optional.of(validInvite()));
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inviteService.acceptInvite(CODE_VALUE, JOINER_ID, NOW))
                    .isInstanceOf(RoomNotFoundError.class);
        }
    }

    @Nested
    class RevokeInvite {

        @Test
        void givenOwner_deletesAndPublishesEvent() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());
            when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(validInvite()));

            inviteService.revokeInvite(ROOM_ID, OWNER_ID, INVITE_ID);

            verify(inviteRepository).delete(any(Invite.class));

            ArgumentCaptor<InviteEvents> captor = ArgumentCaptor.forClass(InviteEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(InviteEvents.EventType.REVOKED);
        }

        @Test
        void givenNonOwner_throwsForbiddenError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> inviteService.revokeInvite(ROOM_ID, MEMBER_ID, INVITE_ID))
                    .isInstanceOf(ForbiddenError.class);

            verify(inviteRepository, never()).delete(any());
        }

        @Test
        void givenUnknownInvite_throwsInvalidInviteCodeError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());
            when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inviteService.revokeInvite(ROOM_ID, OWNER_ID, INVITE_ID))
                    .isInstanceOf(InvalidInviteCodeError.class);
        }

        @Test
        void givenInviteForDifferentRoom_throwsInvalidInviteCodeError() {
            Long otherRoomId = 99L;
            InviteCode code = new InviteCode(CODE_VALUE, OWNER_ID);
            Invite inviteForOtherRoom = Invite.reconstitute(INVITE_ID, otherRoomId, OWNER_ID, code,
                    NOW, NOW.plusSeconds(3600 * 24));

            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());
            when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(inviteForOtherRoom));

            assertThatThrownBy(() -> inviteService.revokeInvite(ROOM_ID, OWNER_ID, INVITE_ID))
                    .isInstanceOf(InvalidInviteCodeError.class);
        }
    }
}
