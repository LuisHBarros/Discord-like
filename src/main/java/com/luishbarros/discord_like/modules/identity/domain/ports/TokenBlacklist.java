package com.luishbarros.discord_like.modules.identity.domain.ports;

public interface TokenBlacklist {
    void add(String token);
    boolean isBlacklisted(String token);
}