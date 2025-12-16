package com.study.security;

/**
 * Interface for password encoding and verification
 */
public interface PasswordEncoder {
    /**
     * Hash password with salt
     * @param rawPassword raw password
     * @param salt salt value
     * @return hashed password
     */
    String encode(String rawPassword, String salt);

    /**
     * Generate random salt
     * @return salt string
     */
    String generateSalt();

    /**
     * Verify password
     * @param rawPassword raw password
     * @param encodedPassword encoded password
     * @param salt salt value
     * @return true if match
     */
    boolean matches(String rawPassword, String encodedPassword, String salt);
}
