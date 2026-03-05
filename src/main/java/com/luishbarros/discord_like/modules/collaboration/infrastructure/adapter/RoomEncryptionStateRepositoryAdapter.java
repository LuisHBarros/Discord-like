package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomEncryptionState;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomEncryptionStateRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomEncryptionStateJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.SpringDataRoomEncryptionStateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RoomEncryptionStateRepositoryAdapter implements RoomEncryptionStateRepository {

    private final SpringDataRoomEncryptionStateRepository jpaRepository;

    public RoomEncryptionStateRepositoryAdapter(SpringDataRoomEncryptionStateRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RoomEncryptionState> findByRoomId(Long roomId) {
        return jpaRepository.findByRoomId(roomId)
                .map(RoomEncryptionStateJpaEntity::toDomain);
    }

    @Override
    public RoomEncryptionState save(RoomEncryptionState state) {
        RoomEncryptionStateJpaEntity entity = RoomEncryptionStateJpaEntity.fromDomain(state);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }
}
