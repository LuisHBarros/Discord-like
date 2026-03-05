package com.luishbarros.discord_like.modules.identity.application.dto;

import com.luishbarros.discord_like.modules.identity.domain.model.User;

public record UserResponse(
        Long id,
        String username,
        String email
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(user.getId(), user.getUsername().value(), user.getEmail().value());
    }
}