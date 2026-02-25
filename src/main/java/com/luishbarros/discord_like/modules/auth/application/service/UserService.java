package com.luishbarros.discord_like.modules.auth.application.service;

import com.luishbarros.discord_like.modules.auth.application.dto.UserResponse;
import com.luishbarros.discord_like.modules.auth.domain.model.User;
import com.luishbarros.discord_like.modules.auth.domain.model.error.UserNotFoundError;
import com.luishbarros.discord_like.modules.auth.domain.ports.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundError(id));

        return UserResponse.fromUser(user);
    }
    public UserResponse getByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundError(email));
        return UserResponse.fromUser(user);
    }
}