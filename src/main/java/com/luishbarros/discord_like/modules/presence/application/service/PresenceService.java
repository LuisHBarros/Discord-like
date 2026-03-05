package com.luishbarros.discord_like.modules.presence.application.service;

import com.luishbarros.discord_like.modules.presence.application.dto.PresenceStatus;
import com.luishbarros.discord_like.modules.presence.application.ports.in.QueryPresenceUseCase;
import com.luishbarros.discord_like.modules.presence.application.ports.in.TrackPresenceUseCase;
import com.luishbarros.discord_like.modules.presence.domain.event.PresenceEvents;
import com.luishbarros.discord_like.modules.presence.domain.model.aggregate.UserPresence;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;
import com.luishbarros.discord_like.modules.presence.domain.ports.repository.PresenceRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.modules.identity.domain.model.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PresenceService implements TrackPresenceUseCase, QueryPresenceUseCase {

    private final PresenceRepository presenceRepository;
    private final EventPublisher eventPublisher;

    public PresenceService(PresenceRepository presenceRepository, EventPublisher eventPublisher) {
        this.presenceRepository = presenceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void setOnline(Long userId) {
        boolean isNewPresence = !presenceRepository.findByUserId(userId).isPresent();
        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElseGet(() -> new UserPresence(userId, PresenceState.ONLINE, Instant.now()));

        PresenceState previousState = presence.getState();
        presence.setOnline();
        presenceRepository.save(presence);

        // Publish event if state changed or new presence created
        if (isNewPresence || previousState != PresenceState.ONLINE) {
            eventPublisher.publish(PresenceEvents.userCameOnline(presence, Instant.now()));
        }
    }

    @Override
    @Transactional
    public void setOffline(Long userId) {
        boolean isNewPresence = !presenceRepository.findByUserId(userId).isPresent();
        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElse(new UserPresence(userId, PresenceState.OFFLINE, Instant.now()));

        PresenceState previousState = presence.getState();
        presence.setOffline();
        presenceRepository.save(presence);

        // Publish event if state changed or new presence created
        if (isNewPresence || previousState != PresenceState.OFFLINE) {
            eventPublisher.publish(PresenceEvents.userWentOffline(presence, Instant.now()));
        }
    }

    @Override
    @Transactional
    public void setPresenceState(Long userId, PresenceState state) {
        boolean isNewPresence = !presenceRepository.findByUserId(userId).isPresent();
        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElseGet(() -> new UserPresence(userId, state, Instant.now()));

        PresenceState previousState = presence.getState();
        presence.setState(state);
        presenceRepository.save(presence);

        // Publish event if state changed or new presence created
        if (isNewPresence || previousState != state) {
            eventPublisher.publish(PresenceEvents.userStateChanged(presence, Instant.now()));
        }
    }

    @Override
    @Transactional
    public void updateLastActivity(Long userId) {
        presenceRepository.findByUserId(userId).ifPresent(presence -> {
            presence.updateLastActivity();
            presenceRepository.save(presence);
        });
    }

    @Override
    public PresenceStatus getPresenceStatus(Long userId) {
        return presenceRepository.findByUserId(userId)
            .map(p -> PresenceStatus.fromDomain(
                p.getUserId(),
                p.getState().name(),
                p.getLastSeen().timestamp(),
                null // username would be fetched from user service
            ))
            .orElse(PresenceStatus.fromDomain(
                userId,
                PresenceState.OFFLINE.name(),
                Instant.now(),
                null
            ));
    }

    @Override
    public Set<PresenceStatus> getOnlineUsers() {
        Set<Long> onlineUserIds = presenceRepository.getOnlineUserIds();
        return onlineUserIds.stream()
            .map(userId -> presenceRepository.findByUserId(userId).orElse(null))
            .filter(p -> p != null)
            .map(p -> PresenceStatus.fromDomain(
                p.getUserId(),
                p.getState().name(),
                p.getLastSeen().timestamp(),
                null
            ))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        return presenceRepository.getOnlineUserIds();
    }
}
