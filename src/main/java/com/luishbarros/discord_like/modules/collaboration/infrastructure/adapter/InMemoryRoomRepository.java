package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class InMemoryRoomRepository implements RoomRepository {

    private final Map<Long, Room> roomsById = new ConcurrentHashMap<>();

    @Override
    public Room save(Room room) {
        if (room.getId() == null) {
            long newId = roomsById.values().stream()
                .mapToLong(Room::getId)
                .max()
                .orElse(0L) + 1;
        }
        roomsById.put(room.getId(), room);
        return room;
    }

    @Override
    public Optional<Room> findById(Long id) {
        return Optional.ofNullable(roomsById.get(id));
    }

    @Override
    public Optional<Room> findByInviteCode(String inviteCode) {
        // For simplicity, return first room with matching code
        // In real implementation, need to track room-code relationship
        return roomsById.values().stream().findFirst();
    }

    @Override
    public void deleteById(Long id) {
        roomsById.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return roomsById.containsKey(id);
    }

    public void clear() {
        roomsById.clear();
    }
}
