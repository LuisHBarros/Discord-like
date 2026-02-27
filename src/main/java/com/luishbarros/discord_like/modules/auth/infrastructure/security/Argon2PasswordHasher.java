// auth/infrastructure/security/Argon2PasswordHasher.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.ports.PasswordHasher;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64MB
    private static final int PARALLELISM = 1;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String hash(String plaintext) {
        byte[] salt = generateSalt();
        byte[] hash = argon2Hash(plaintext.getBytes(StandardCharsets.UTF_8), salt);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
                MEMORY, ITERATIONS, PARALLELISM, saltBase64, hashBase64);
    }

    @Override
    public boolean verify(String plaintext, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 6 || !"argon2id".equals(parts[1])) return false;


        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = argon2Hash(plaintext.getBytes(StandardCharsets.UTF_8), salt);

        return slowEquals(expectedHash, actualHash);
    }

    private byte[] argon2Hash(byte[] password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY)
                .withParallelism(PARALLELISM)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(password, hash);
        return hash;
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        this.secureRandom.nextBytes(salt);
        return salt;
    }

    // Constant-time comparison to prevent timing attacks
    private boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}