package com.luishbarros.discord_like.modules.collaboration.domain.ports.repository;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;

import java.util.Optional;

public interface RoomEncryptionStateRepository {
    Optional<RoomEncryptionState> findByRoomId(Long roomId);
    RoomEncryptionState save(RoomEncryptionState state);
    void deleteByRoomId(Long roomId);
}
