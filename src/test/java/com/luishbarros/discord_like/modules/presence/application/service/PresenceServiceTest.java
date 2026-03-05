package com.luishbarros.discord_like.modules.presence.application.service;

import com.luishbarros.discord_like.modules.presence.application.dto.PresenceStatus;
import com.luishbarros.discord_like.modules.presence.domain.event.PresenceEvents;
import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.LastSeen;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;
import com.luishbarros.discord_like.modules.presence.domain.ports.repository.PresenceRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock private PresenceRepository presenceRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private PresenceService presenceService;

    private static final Long USER_ID = 1L;

    private UserPresence createPresence(Long userId, PresenceState state, Instant lastSeen) {
        return UserPresence.reconstitute(1L, userId, state, new LastSeen(lastSeen));
    }

    // ─── SetOnline ────────────────────────────────────────────────────────────

    @Nested
    class SetOnline {

        @Test
        void givenExistingPresenceWithDifferentState_updatesToOnlineAndPublishesEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence offlinePresence = createPresence(USER_ID, PresenceState.OFFLINE, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(offlinePresence));
            when(presenceRepository.save(any())).thenReturn(offlinePresence);

            presenceService.setOnline(USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.ONLINE);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_CAME_ONLINE);
            assertThat(event.userId()).isEqualTo(USER_ID);
        }

        @Test
        void givenNoPresence_createsNewOnlinePresenceAndPublishesEvent() {
            UserPresence newPresence = createPresence(USER_ID, PresenceState.ONLINE, Instant.now());
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(presenceRepository.save(any())).thenReturn(newPresence);

            presenceService.setOnline(USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.ONLINE);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_CAME_ONLINE);
        }

        @Test
        void givenAlreadyOnline_updatesLastSeenButDoesNotPublishEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence onlinePresence = createPresence(USER_ID, PresenceState.ONLINE, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(onlinePresence));
            when(presenceRepository.save(any())).thenReturn(onlinePresence);

            presenceService.setOnline(USER_ID);

            verify(presenceRepository).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ─── SetOffline ────────────────────────────────────────────────────────────

    @Nested
    class SetOffline {

        @Test
        void givenOnlinePresence_updatesToOfflineAndPublishesEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence onlinePresence = createPresence(USER_ID, PresenceState.ONLINE, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(onlinePresence));
            when(presenceRepository.save(any())).thenReturn(onlinePresence);

            presenceService.setOffline(USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.OFFLINE);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_WENT_OFFLINE);
            assertThat(event.userId()).isEqualTo(USER_ID);
        }

        @Test
        void givenNoPresence_createsNewOfflinePresenceAndPublishesEvent() {
            UserPresence newPresence = createPresence(USER_ID, PresenceState.OFFLINE, Instant.now());
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(presenceRepository.save(any())).thenReturn(newPresence);

            presenceService.setOffline(USER_ID);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.OFFLINE);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_WENT_OFFLINE);
        }

        @Test
        void givenAlreadyOffline_updatesLastSeenButDoesNotPublishEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence offlinePresence = createPresence(USER_ID, PresenceState.OFFLINE, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(offlinePresence));
            when(presenceRepository.save(any())).thenReturn(offlinePresence);

            presenceService.setOffline(USER_ID);

            verify(presenceRepository).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ─── SetPresenceState ─────────────────────────────────────────────────────

    @Nested
    class SetPresenceState {

        @Test
        void givenDifferentState_updatesStateAndPublishesEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence presence = createPresence(USER_ID, PresenceState.AWAY, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(presence));
            when(presenceRepository.save(any())).thenReturn(presence);

            presenceService.setPresenceState(USER_ID, PresenceState.BUSY);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.BUSY);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_STATE_CHANGED);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.state()).isEqualTo(PresenceState.BUSY);
        }

        @Test
        void givenNoPresence_createsNewPresenceWithGivenStateAndPublishesEvent() {
            UserPresence newPresence = createPresence(USER_ID, PresenceState.AWAY, Instant.now());
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(presenceRepository.save(any())).thenReturn(newPresence);

            presenceService.setPresenceState(USER_ID, PresenceState.AWAY);

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(presenceRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PresenceState.AWAY);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);

            ArgumentCaptor<PresenceEvents> eventCaptor = ArgumentCaptor.forClass(PresenceEvents.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            PresenceEvents event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(PresenceEvents.EventType.USER_STATE_CHANGED);
        }

        @Test
        void givenSameState_updatesLastSeenButDoesNotPublishEvent() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence presence = createPresence(USER_ID, PresenceState.AWAY, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(presence));
            when(presenceRepository.save(any())).thenReturn(presence);

            presenceService.setPresenceState(USER_ID, PresenceState.AWAY);

            verify(presenceRepository).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ─── UpdateLastActivity ──────────────────────────────────────────────────

    @Nested
    class UpdateLastActivity {

        @Test
        void givenExistingPresence_updatesLastSeen() {
            Instant before = Instant.now().minusSeconds(60);
            UserPresence presence = createPresence(USER_ID, PresenceState.ONLINE, before);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(presence));
            when(presenceRepository.save(any())).thenReturn(presence);

            presenceService.updateLastActivity(USER_ID);

            verify(presenceRepository).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void givenNoPresence_doesNothing() {
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            presenceService.updateLastActivity(USER_ID);

            verify(presenceRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ─── GetPresenceStatus ────────────────────────────────────────────────────

    @Nested
    class GetPresenceStatus {

        @Test
        void givenExistingPresence_returnsMatchingStatus() {
            Instant lastSeen = Instant.now();
            UserPresence presence = createPresence(USER_ID, PresenceState.ONLINE, lastSeen);
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(presence));

            PresenceStatus status = presenceService.getPresenceStatus(USER_ID);

            assertThat(status.userId()).isEqualTo(USER_ID);
            assertThat(status.state()).isEqualTo(PresenceState.ONLINE.name());
            assertThat(status.lastSeenAt()).isEqualTo(lastSeen);
        }

        @Test
        void givenNoPresence_returnsOfflineStatus() {
            when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            PresenceStatus status = presenceService.getPresenceStatus(USER_ID);

            assertThat(status.userId()).isEqualTo(USER_ID);
            assertThat(status.state()).isEqualTo(PresenceState.OFFLINE.name());
        }
    }

    // ─── GetOnlineUsers ───────────────────────────────────────────────────────

    @Nested
    class GetOnlineUsers {

        @Test
        void givenOnlineUsers_returnsTheirStatuses() {
            Long user1 = 1L;
            Long user2 = 2L;
            Instant lastSeen = Instant.now();

            UserPresence presence1 = createPresence(user1, PresenceState.ONLINE, lastSeen);
            UserPresence presence2 = createPresence(user2, PresenceState.ONLINE, lastSeen);

            when(presenceRepository.getOnlineUserIds()).thenReturn(Set.of(user1, user2));
            when(presenceRepository.findByUserId(user1)).thenReturn(Optional.of(presence1));
            when(presenceRepository.findByUserId(user2)).thenReturn(Optional.of(presence2));

            Set<PresenceStatus> statuses = presenceService.getOnlineUsers();

            assertThat(statuses).hasSize(2);
            assertThat(statuses.stream().map(PresenceStatus::userId)).containsExactlyInAnyOrder(user1, user2);
        }

        @Test
        void givenNoOnlineUsers_returnsEmptySet() {
            when(presenceRepository.getOnlineUserIds()).thenReturn(Set.of());

            Set<PresenceStatus> statuses = presenceService.getOnlineUsers();

            assertThat(statuses).isEmpty();
        }

        @Test
        void givenPresenceNotFoundForUserId_filtersItOut() {
            Long user1 = 1L;
            Long user2 = 2L;

            UserPresence presence1 = createPresence(user1, PresenceState.ONLINE, Instant.now());

            when(presenceRepository.getOnlineUserIds()).thenReturn(Set.of(user1, user2));
            when(presenceRepository.findByUserId(user1)).thenReturn(Optional.of(presence1));
            when(presenceRepository.findByUserId(user2)).thenReturn(Optional.empty());

            Set<PresenceStatus> statuses = presenceService.getOnlineUsers();

            assertThat(statuses).hasSize(1);
            assertThat(statuses.stream().map(PresenceStatus::userId)).containsExactly(user1);
        }
    }

    // ─── GetOnlineUserIds ─────────────────────────────────────────────────────

    @Nested
    class GetOnlineUserIds {

        @Test
        void givenOnlineUsers_returnsTheirIds() {
            Set<Long> onlineIds = Set.of(1L, 2L, 3L);
            when(presenceRepository.getOnlineUserIds()).thenReturn(onlineIds);

            Set<Long> result = presenceService.getOnlineUserIds();

            assertThat(result).isEqualTo(onlineIds);
        }

        @Test
        void givenNoOnlineUsers_returnsEmptySet() {
            when(presenceRepository.getOnlineUserIds()).thenReturn(Set.of());

            Set<Long> result = presenceService.getOnlineUserIds();

            assertThat(result).isEmpty();
        }
    }
}
