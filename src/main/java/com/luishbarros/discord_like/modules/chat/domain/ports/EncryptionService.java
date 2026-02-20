package com.luishbarros.discord_like.modules.chat.domain.ports;

public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String plaintext);
}
