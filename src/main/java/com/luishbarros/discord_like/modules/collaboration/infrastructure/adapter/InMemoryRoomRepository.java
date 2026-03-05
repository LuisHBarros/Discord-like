package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoomRepository implements RoomRepository {

    private final Map<Long, Room> roomsById = new ConcurrentHashMap<>();
    private final Map<Long, Set<Room>> roomsByMemberId = new ConcurrentHashMap<>();

    @Override
    public Room save(Room room) {
        if (room.getId() == null) {
            long newId = roomsById.values().stream()
                .mapToLong(Room::getId)
                .max()
                .orElse(0L) + 1;
        }
        roomsById.put(room.getId(), room);
        room.getMemberIds().forEach(memberId -> {
            roomsByMemberId.computeIfAbsent(memberId, k -> new HashSet<>()).add(room);
        });
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
    public List<Room> findByMemberId(Long memberId) {
        return roomsByMemberId.getOrDefault(memberId, Set.of())
            .stream()
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        Room room = roomsById.remove(id);
        if (room != null) {
            room.getMemberIds().forEach(memberId -> {
                Set<Room> userRooms = roomsByMemberId.get(memberId);
                if (userRooms != null) {
                    userRooms.remove(room);
                }
            });
        }
    }

    @Override
    public boolean existsById(Long id) {
        return roomsById.containsKey(id);
    }

    public void clear() {
        roomsById.clear();
        roomsByMemberId.clear();
    }
}
