package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;
    private final RoomMembershipValidator membershipValidator;

    public RoomService(
            RoomRepository roomRepository,
            EventPublisher eventPublisher,
            RoomMembershipValidator membershipValidator
    ) {
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
        this.membershipValidator = membershipValidator;
    }

    public Room createRoom(String name, Long ownerId, Instant now) {
        Room room = new Room(name, ownerId, now);
        Room saved = roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }

    public Room findById(Long roomId, Long userId) {
        return membershipValidator.validateAndGetRoom(roomId, userId);
    }

    public List<Room> findByMemberId(Long userId) {
        return roomRepository.findByMemberId(userId);
    }

    public Set<Long> getMembers(Long roomId, Long userId) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        return room.getMemberIds();
    }

    public Room updateRoomName(Long roomId, Long userId, String newName, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        room.setName(newName, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomUpdated(roomId, userId, now));
        return room;
    }

    public void deleteRoom(Long roomId, Long userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        roomRepository.deleteById(roomId);
        eventPublisher.publish(RoomEvents.roomDeleted(roomId, userId, now));
    }

    public void addMember(Long roomId, Long invitedUserId, Instant now) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));
        room.addMember(invitedUserId, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.memberJoined(roomId, invitedUserId, now));
    }

    public void removeMember(Long roomId, Long userId, Long targetUserId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);
        room.removeMember(targetUserId, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.memberLeft(roomId, targetUserId, now));
    }

    public void leaveRoom(Long roomId, Long userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        if (room.isOwner(userId)) {
            throw new InvalidRoomError("Room owner cannot leave room");
        }
        room.removeMember(userId, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.memberLeft(roomId, userId, now));
    }

    private void validateOwnership(Room room, Long userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can perform this action");
        }
    }
}