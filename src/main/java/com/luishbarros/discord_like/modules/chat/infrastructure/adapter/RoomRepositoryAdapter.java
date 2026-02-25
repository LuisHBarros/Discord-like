package com.luishbarros.discord_like.modules.chat.infrastructure.adapter;

import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.RoomJpaEntity;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository.RoomJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RoomRepositoryAdapter implements RoomRepository {

    private final RoomJpaRepository jpaRepository;

    public RoomRepositoryAdapter(RoomJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Room save(Room room) {
        RoomJpaEntity entity = RoomJpaEntity.fromDomain(room);
        RoomJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return jpaRepository.findById(id)
                .map(RoomJpaEntity::toDomain);
    }

    @Override
    public Optional<Room> findByInviteCode(String inviteCode) {
        return jpaRepository.findByInviteCode(inviteCode)
                .map(RoomJpaEntity::toDomain);
    }

    @Override
    public List<Room> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).stream()
                .map(RoomJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
