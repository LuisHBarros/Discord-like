// auth/infrastructure/adapter/JpaUserRepository.java
package com.luishbarros.discord_like.modules.identity.infrastructure.adapter;

import com.luishbarros.discord_like.modules.identity.domain.model.User;
import com.luishbarros.discord_like.modules.identity.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.modules.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.luishbarros.discord_like.modules.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository jpa;

    public JpaUserRepository(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}