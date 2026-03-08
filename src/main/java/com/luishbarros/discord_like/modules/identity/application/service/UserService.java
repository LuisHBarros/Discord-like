package com.luishbarros.discord_like.modules.identity.application.service;

import com.luishbarros.discord_like.modules.identity.application.dto.UserResponse;
import com.luishbarros.discord_like.modules.identity.domain.model.User;
import com.luishbarros.discord_like.modules.identity.domain.model.error.UserNotFoundError;
import com.luishbarros.discord_like.modules.identity.domain.ports.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(
            value = "users",
            key = "#id",
            unless = "#result == null"
    )
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundError(id));

        return UserResponse.fromUser(user);
    }

    @Cacheable(
            value = "users-by-email",
            key = "#email",
            unless = "#result == null"
    )
    public UserResponse getByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundError(email));
        return UserResponse.fromUser(user);
    }
}