package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.InviteRepository;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.InviteJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.InviteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InviteRepositoryAdapter implements InviteRepository {

    private final InviteJpaRepository jpaRepository;

    public InviteRepositoryAdapter(InviteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invite save(Invite invite) {
        InviteJpaEntity saved = jpaRepository.save(InviteJpaEntity.fromDomain(invite));
        return saved.toDomain();
    }

    @Override
    public Optional<Invite> findById(Long id) {
        return jpaRepository.findById(id)
                .map(InviteJpaEntity::toDomain);
    }

    @Override
    public Optional<Invite> findByCode(String codeValue) {
        return jpaRepository.findByCodeValue(codeValue)
                .map(InviteJpaEntity::toDomain);
    }

    @Override
    public void delete(Invite invite) {
        jpaRepository.deleteById(invite.getId());
    }
}
