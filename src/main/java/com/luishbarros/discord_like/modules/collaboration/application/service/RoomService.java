package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.collaboration.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.RoomName;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;
    private final RoomMembershipValidator membershipValidator;
    private final RoomMembershipRepository roomMembershipRepository;

    public RoomService(
            RoomRepository roomRepository,
            EventPublisher eventPublisher,
            RoomMembershipValidator membershipValidator,
            RoomMembershipRepository roomMembershipRepository
    ) {
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
        this.membershipValidator = membershipValidator;
        this.roomMembershipRepository = roomMembershipRepository;
    }

    public Room createRoom(String name, Long ownerId, Instant now) {
        Room room = new Room(new RoomName(name), ownerId, now);
        Room saved = roomRepository.save(room);
        
        RoomMembership ownerMembership = RoomMembership.create(saved.getId(), ownerId, now);
        roomMembershipRepository.save(ownerMembership);
        
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }

    @Cacheable(
            value = "rooms",
            key = "#roomId + ':' + #userId",
            unless = "#result == null"
    )
    public Room findById(Long roomId, Long userId) {
        return membershipValidator.validateAndGetRoom(roomId, userId);
    }

    @Cacheable(
            value = "user-rooms",
            key = "#userId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Room> findByMemberId(Long userId) {
        return roomMembershipRepository.findByUserId(userId).stream()
                .map(membership -> roomRepository.findById(membership.getRoomId()).orElse(null))
                .filter(room -> room != null)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "room-members",
            key = "#roomId",
            unless = "#result == null || #result.isEmpty()"
    )
    public Set<Long> getMembers(Long roomId, Long userId) {
        membershipValidator.validateAndGetRoom(roomId, userId);
        return roomMembershipRepository.findByRoomId(roomId).stream()
                .map(RoomMembership::getUserId)
                .collect(Collectors.toSet());
    }

    @CachePut(
            value = "rooms",
            key = "#roomId + ':' + #userId"
    )
    public Room updateRoomName(Long roomId, Long userId, String newName, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        room.setName(new RoomName(newName), now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomUpdated(roomId, userId, now));
        return room;
    }

    @CacheEvict(
            value = {"rooms", "room-members", "user-rooms"},
            key = "#roomId"
    )
    public void deleteRoom(Long roomId, Long userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        roomMembershipRepository.deleteByRoomId(roomId);
        roomRepository.deleteById(roomId);
        eventPublisher.publish(RoomEvents.roomDeleted(roomId, userId, now));
    }

    @CacheEvict(
            value = {"room-members", "user-rooms"},
            key = "#roomId"
    )
    public void addMember(Long roomId, Long invitedUserId, Instant now) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));
        if (!roomMembershipRepository.existsByRoomIdAndUserId(roomId, invitedUserId)) {
            RoomMembership newMembership = RoomMembership.create(roomId, invitedUserId, now);
            roomMembershipRepository.save(newMembership);
        }
        eventPublisher.publish(RoomEvents.memberJoined(roomId, invitedUserId, now));
    }

    @CacheEvict(
            value = {"room-members", "user-rooms"},
            key = "#roomId"
    )
    public void removeMember(Long roomId, Long userId, Long targetUserId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        
        if (room.isOwner(targetUserId)) {
            throw new InvalidRoomError("Cannot remove room owner");
        }
        
        roomMembershipRepository.deleteByRoomIdAndUserId(roomId, targetUserId);
        eventPublisher.publish(RoomEvents.memberLeft(roomId, targetUserId, now));
    }

    @CacheEvict(
            value = {"room-members", "user-rooms"},
            key = "#roomId"
    )
    public void leaveRoom(Long roomId, Long userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        if (room.isOwner(userId)) {
            throw new InvalidRoomError("Room owner cannot leave room");
        }
        roomMembershipRepository.deleteByRoomIdAndUserId(roomId, userId);
        eventPublisher.publish(RoomEvents.memberLeft(roomId, userId, now));
    }

    private void validateOwnership(Room room, Long userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can perform this action");
        }
    }
}