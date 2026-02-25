package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(Long id);

    Optional<Room> findByInviteCode(String inviteCode);

    List<Room> findByMemberId(Long memberId);

    void deleteById(Long id);

    boolean existsById(Long id);
}
