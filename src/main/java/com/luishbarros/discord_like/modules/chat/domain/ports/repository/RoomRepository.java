package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(UUID id);

    Optional<Room> findByInviteCode(String inviteCode);

    List<Room> findByMemberId(UUID memberId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
