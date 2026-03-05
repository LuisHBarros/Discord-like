package com.luishbarros.discord_like.modules.identity.domain.ports;

public interface PasswordHasher {
    String hash(String plaintext);
    boolean verify(String plaintext, String hash);
}