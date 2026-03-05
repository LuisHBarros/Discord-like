package com.luishbarros.discord_like.modules.collaboration.domain.model.value_object;

import java.time.Instant;

public record Membership(Long userId, String role, Instant joinedAt) {
    public enum Role {
        OWNER,
        MEMBER
    }

    public Membership {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (role == null) {
            role = Role.MEMBER.name();
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public static Membership owner(Long userId) {
        return new Membership(userId, Role.OWNER.name(), Instant.now());
    }

    public static Membership member(Long userId) {
        return new Membership(userId, Role.MEMBER.name(), Instant.now());
    }

    public boolean isOwner() {
        return Role.OWNER.name().equals(role);
    }
}
