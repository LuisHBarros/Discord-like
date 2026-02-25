package com.luishbarros.discord_like.modules.auth.domain.ports.repository;

import com.luishbarros.discord_like.modules.auth.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
