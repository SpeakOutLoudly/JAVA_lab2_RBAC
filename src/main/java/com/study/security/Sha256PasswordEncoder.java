package com.study.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SHA-256 based password encoder with salt
 */
public class Sha256PasswordEncoder implements PasswordEncoder {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;

    @Override
    public String encode(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = rawPassword + salt;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword, String salt) {
        String hashedInput = encode(rawPassword, salt);
        return hashedInput.equals(encodedPassword);
    }
}
