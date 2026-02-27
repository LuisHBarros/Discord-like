package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
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
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private RoomMembershipValidator membershipValidator;

    @InjectMocks
    private RoomService roomService;

    private static final Long    ROOM_ID   = 10L;
    private static final Long    OWNER_ID  = 1L;
    private static final Long    MEMBER_ID = 2L;
    private static final Long    OTHER_ID  = 3L;
    private static final Instant NOW       = Instant.parse("2026-01-01T00:00:00Z");

    private Room roomWith2Members() {
        return Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID, MEMBER_ID), NOW, NOW);
    }

    private Room roomWithOwnerOnly() {
        return Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID), NOW, NOW);
    }

    // ─── CreateRoom ───────────────────────────────────────────────────────────

    @Nested
    class CreateRoom {

        @Test
        void savesRoomAndReturnsSavedInstanceWithId() {
            Room savedRoom = Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID), NOW, NOW);
            when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);

            Room result = roomService.createRoom("General", OWNER_ID, NOW);

            ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("General");
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(captor.getValue().getMemberIds()).containsExactly(OWNER_ID);

            // garante que o room retornado é o que vem do repositório (com ID)
            assertThat(result.getId()).isEqualTo(ROOM_ID);
        }

        @Test
        void publishesRoomCreatedEventWithSavedRoomId() {
            Room savedRoom = Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID), NOW, NOW);
            when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);

            roomService.createRoom("General", OWNER_ID, NOW);

            ArgumentCaptor<RoomEvents> captor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(RoomEvents.EventType.ROOM_CREATED);
            // garante que o evento carrega o ID real, não null
            assertThat(captor.getValue().roomId()).isEqualTo(ROOM_ID);
        }

        @Test
        void returnsCreatedRoomWithIdFromRepository() {
            Room savedRoom = Room.reconstitute(ROOM_ID, "General", OWNER_ID, Set.of(OWNER_ID), NOW, NOW);
            when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);

            Room result = roomService.createRoom("General", OWNER_ID, NOW);

            assertThat(result.getId()).isEqualTo(ROOM_ID);
            assertThat(result.getName()).isEqualTo("General");
            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        }
    }

    // ─── FindById ─────────────────────────────────────────────────────────────

    @Nested
    class FindById {

        @Test
        void delegatesToMembershipValidator() {
            Room room = roomWith2Members();
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(room);

            Room result = roomService.findById(ROOM_ID, MEMBER_ID);

            assertThat(result).isEqualTo(room);
        }

        @Test
        void givenNonMember_throwsUserNotInRoomError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OTHER_ID))
                    .thenThrow(new UserNotInRoomError(OTHER_ID.toString(), ROOM_ID.toString()));

            assertThatThrownBy(() -> roomService.findById(ROOM_ID, OTHER_ID))
                    .isInstanceOf(UserNotInRoomError.class);
        }
    }

    // ─── FindByMemberId ───────────────────────────────────────────────────────

    @Nested
    class FindByMemberId {

        @Test
        void returnsRoomsForMember() {
            List<Room> rooms = List.of(roomWith2Members());
            when(roomRepository.findByMemberId(OWNER_ID)).thenReturn(rooms);

            List<Room> result = roomService.findByMemberId(OWNER_ID);

            assertThat(result).isEqualTo(rooms);
        }
    }

    // ─── UpdateRoomName ───────────────────────────────────────────────────────

    @Nested
    class UpdateRoomName {

        @Test
        void givenOwner_savesNewNameAndPublishesEvent() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());

            Room result = roomService.updateRoomName(ROOM_ID, OWNER_ID, "Renamed", NOW);

            assertThat(result.getName()).isEqualTo("Renamed");
            verify(roomRepository).save(result);
            ArgumentCaptor<RoomEvents> captor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(RoomEvents.EventType.ROOM_UPDATED);
        }

        @Test
        void givenNonOwner_throwsForbiddenError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> roomService.updateRoomName(ROOM_ID, MEMBER_ID, "Renamed", NOW))
                    .isInstanceOf(ForbiddenError.class);

            verify(roomRepository, never()).save(any());
        }
    }

    // ─── DeleteRoom ───────────────────────────────────────────────────────────

    @Nested
    class DeleteRoom {

        @Test
        void givenOwner_deletesAndPublishesEvent() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());

            roomService.deleteRoom(ROOM_ID, OWNER_ID, NOW);

            verify(roomRepository).deleteById(ROOM_ID);
            ArgumentCaptor<RoomEvents> captor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(RoomEvents.EventType.ROOM_DELETED);
        }

        @Test
        void givenNonOwner_throwsForbiddenError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> roomService.deleteRoom(ROOM_ID, MEMBER_ID, NOW))
                    .isInstanceOf(ForbiddenError.class);

            verify(roomRepository, never()).deleteById(any());
        }
    }

    // ─── AddMember ────────────────────────────────────────────────────────────

    @Nested
    class AddMember {

        @Test
        void givenExistingRoom_addsMemberSavesAndPublishesEvent() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomWithOwnerOnly()));

            roomService.addMember(ROOM_ID, OTHER_ID, NOW);

            ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getMemberIds()).contains(OTHER_ID);

            ArgumentCaptor<RoomEvents> eventCaptor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().type()).isEqualTo(RoomEvents.EventType.MEMBER_JOINED);
            assertThat(eventCaptor.getValue().userId()).isEqualTo(OTHER_ID);
        }

        @Test
        void givenUnknownRoom_throwsRoomNotFoundError() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.addMember(ROOM_ID, OTHER_ID, NOW))
                    .isInstanceOf(RoomNotFoundError.class);
        }
    }

    // ─── RemoveMember ─────────────────────────────────────────────────────────

    @Nested
    class RemoveMember {

        @Test
        void givenOwnerRemovesNonOwner_savesAndPublishesEvent() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());

            roomService.removeMember(ROOM_ID, OWNER_ID, MEMBER_ID, NOW);

            ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getMemberIds()).doesNotContain(MEMBER_ID);

            ArgumentCaptor<RoomEvents> eventCaptor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().type()).isEqualTo(RoomEvents.EventType.MEMBER_LEFT);
            assertThat(eventCaptor.getValue().userId()).isEqualTo(MEMBER_ID);
        }

        @Test
        void givenNonOwnerCaller_throwsForbiddenError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> roomService.removeMember(ROOM_ID, MEMBER_ID, OTHER_ID, NOW))
                    .isInstanceOf(ForbiddenError.class);
        }

        @Test
        void givenOwnerTriesToRemoveOwner_throwsInvalidRoomError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> roomService.removeMember(ROOM_ID, OWNER_ID, OWNER_ID, NOW))
                    .isInstanceOf(InvalidRoomError.class);
        }
    }

    // ─── LeaveRoom ────────────────────────────────────────────────────────────

    @Nested
    class LeaveRoom {

        @Test
        void givenNonOwnerMember_removesAndPublishesEvent() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, MEMBER_ID)).thenReturn(roomWith2Members());

            roomService.leaveRoom(ROOM_ID, MEMBER_ID, NOW);

            ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getMemberIds()).doesNotContain(MEMBER_ID);

            ArgumentCaptor<RoomEvents> eventCaptor = ArgumentCaptor.forClass(RoomEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().type()).isEqualTo(RoomEvents.EventType.MEMBER_LEFT);
            assertThat(eventCaptor.getValue().userId()).isEqualTo(MEMBER_ID);
        }

        @Test
        void givenOwner_throwsInvalidRoomError() {
            when(membershipValidator.validateAndGetRoom(ROOM_ID, OWNER_ID)).thenReturn(roomWith2Members());

            assertThatThrownBy(() -> roomService.leaveRoom(ROOM_ID, OWNER_ID, NOW))
                    .isInstanceOf(InvalidRoomError.class);

            verify(roomRepository, never()).save(any());
        }
    }
}