package com.luishbarros.discord_like.modules.collaboration.domain.ports.repository;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;

import java.util.List;
import java.util.Optional;

public interface RoomMembershipRepository {

    RoomMembership save(RoomMembership roomMembership);

    Optional<RoomMembership> findById(Long id);

    Optional<RoomMembership> findByRoomIdAndUserId(Long roomId, Long userId);

    List<RoomMembership> findByRoomId(Long roomId);

    List<RoomMembership> findByUserId(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    void deleteByRoomIdAndUserId(Long roomId, Long userId);
    
    void deleteByRoomId(Long roomId);
}
