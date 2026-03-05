package com.luishbarros.discord_like.modules.identity.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.identity.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByUsername(String username);
    boolean existsByEmail(String email);
}