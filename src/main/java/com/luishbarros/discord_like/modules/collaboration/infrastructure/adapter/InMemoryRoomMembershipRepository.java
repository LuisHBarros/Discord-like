package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("test")
public class InMemoryRoomMembershipRepository implements RoomMembershipRepository {

    private final ConcurrentHashMap<Long, RoomMembership> memberships = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public RoomMembership save(RoomMembership roomMembership) {
        RoomMembership saved;
        if (roomMembership.getId() == null) {
            saved = RoomMembership.reconstitute(
                    idGenerator.getAndIncrement(),
                    roomMembership.getRoomId(),
                    roomMembership.getUserId(),
                    roomMembership.getJoinedAt(),
                    roomMembership.getEncryptedRoomKey()
            );
        } else {
            saved = roomMembership;
        }
        memberships.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public Optional<RoomMembership> findById(Long id) {
        return Optional.ofNullable(memberships.get(id));
    }

    @Override
    public Optional<RoomMembership> findByRoomIdAndUserId(Long roomId, Long userId) {
        return memberships.values().stream()
                .filter(m -> m.getRoomId().equals(roomId) && m.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<RoomMembership> findByRoomId(Long roomId) {
        return memberships.values().stream()
                .filter(m -> m.getRoomId().equals(roomId))
                .toList();
    }

    @Override
    public List<RoomMembership> findByUserId(Long userId) {
        return memberships.values().stream()
                .filter(m -> m.getUserId().equals(userId))
                .toList();
    }

    @Override
    public boolean existsByRoomIdAndUserId(Long roomId, Long userId) {
        return findByRoomIdAndUserId(roomId, userId).isPresent();
    }

    @Override
    public void deleteByRoomIdAndUserId(Long roomId, Long userId) {
        findByRoomIdAndUserId(roomId, userId).ifPresent(m -> memberships.remove(m.getId()));
    }
    
    @Override
    public void deleteByRoomId(Long roomId) {
        List<RoomMembership> toDelete = findByRoomId(roomId);
        toDelete.forEach(m -> memberships.remove(m.getId()));
    }
    
    public void clear() {
        memberships.clear();
    }
}
