package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.ports.EventPublisher;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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

    public Room createRoom(String name, UUID ownerId, Instant now) {
        Room room = new Room(name, ownerId, now);

        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomCreated(room, now));

        return room;
    }

    public Room updateRoomName(UUID roomId, UUID userId, String newName, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);

        validateOwnership(room, userId);

        room.setName(newName, now);
        roomRepository.save(room);

        eventPublisher.publish(RoomEvents.roomUpdated(roomId, userId, now));

        return room;
    }

    public void addMember(UUID roomId, UUID invitedUserId, Instant now) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));

        room.addMember(invitedUserId, now);
        roomRepository.save(room);

        eventPublisher.publish(RoomEvents.memberJoined(roomId, invitedUserId, now));
    }

    public void removeMember(UUID roomId, UUID userId, UUID targetUserId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);

        validateOwnership(room, userId);

        room.removeMember(targetUserId, now);
        roomRepository.save(room);

        eventPublisher.publish(RoomEvents.memberLeft(roomId, targetUserId, now));
    }

    public void leaveRoom(UUID roomId, UUID userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);

        if (room.isOwner(userId)) {
            throw new InvalidRoomError("Room owner cannot leave room");
        }

        room.removeMember(userId, now);
        roomRepository.save(room);

        eventPublisher.publish(RoomEvents.memberLeft(roomId, userId, now));
    }

    private void validateOwnership(Room room, UUID userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can perform this action");
        }
    }
}
