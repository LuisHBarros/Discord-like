package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomMembershipJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.RoomMembershipJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
public class RoomMembershipRepositoryAdapter implements RoomMembershipRepository {

    private final RoomMembershipJpaRepository jpaRepository;

    public RoomMembershipRepositoryAdapter(RoomMembershipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RoomMembership save(RoomMembership roomMembership) {
        RoomMembershipJpaEntity entity = RoomMembershipJpaEntity.fromDomain(roomMembership);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<RoomMembership> findById(Long id) {
        return jpaRepository.findById(id).map(RoomMembershipJpaEntity::toDomain);
    }

    @Override
    public Optional<RoomMembership> findByRoomIdAndUserId(Long roomId, Long userId) {
        return jpaRepository.findByRoomIdAndUserId(roomId, userId).map(RoomMembershipJpaEntity::toDomain);
    }

    @Override
    public List<RoomMembership> findByRoomId(Long roomId) {
        return jpaRepository.findByRoomId(roomId).stream()
                .map(RoomMembershipJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<RoomMembership> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(RoomMembershipJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRoomIdAndUserId(Long roomId, Long userId) {
        return jpaRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    @Override
    public void deleteByRoomIdAndUserId(Long roomId, Long userId) {
        jpaRepository.deleteByRoomIdAndUserId(roomId, userId);
    }
    
    @Override
    public void deleteByRoomId(Long roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }
}
