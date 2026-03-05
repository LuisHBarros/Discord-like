package com.luishbarros.discord_like.modules.identity.infrastructure.adapter;

import com.luishbarros.discord_like.modules.identity.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.modules.identity.domain.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            // Generate synthetic ID for in-memory storage
            long newId = usersById.values().stream()
                .mapToLong(User::getId)
                .max()
                .orElse(0L) + 1;
        }
        usersById.put(user.getId(), user);
        usersByEmail.put(user.getEmail().value(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }

    public void clear() {
        usersById.clear();
        usersByEmail.clear();
    }
}
