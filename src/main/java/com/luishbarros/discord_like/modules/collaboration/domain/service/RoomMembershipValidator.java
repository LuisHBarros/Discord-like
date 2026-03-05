package com.luishbarros.discord_like.modules.collaboration.domain.service;

import com.luishbarros.discord_like.modules.collaboration.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomMembershipValidator {

    private final RoomRepository roomRepository;

    public RoomMembershipValidator(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Validates that a user is a member of a room and returns the room.
     * Note: Cache is managed at the service layer (RoomService) to ensure
     * consistent cache key strategy across the application.
     */
    public Room validateAndGetRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));

        if (!room.isMember(userId)) {
            throw new UserNotInRoomError(userId.toString(), roomId.toString());
        }

        return room;
    }


}
