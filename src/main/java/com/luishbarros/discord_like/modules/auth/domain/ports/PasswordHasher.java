package com.luishbarros.discord_like.modules.auth.domain.ports;

public interface PasswordHasher {
    String hash(String plaintext);
    boolean verify(String plaintext, String hash);
}