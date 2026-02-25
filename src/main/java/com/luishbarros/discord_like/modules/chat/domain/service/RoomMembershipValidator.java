package com.luishbarros.discord_like.modules.chat.domain.service;

import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.error.UserNotInRoomError;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomMembershipValidator {

    private final RoomRepository roomRepository;

    public RoomMembershipValidator(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room validateAndGetRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));

        if (!room.isMember(userId)) {
            throw new UserNotInRoomError(userId.toString(), roomId.toString());
        }

        return room;
    }


}
