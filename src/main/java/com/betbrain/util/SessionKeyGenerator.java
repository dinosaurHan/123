package com.betbrain.util;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * Generates cryptographically-secure random session keys
 */
public class SessionKeyGenerator {
    private static final String CHARACTER_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int DEFAULT_KEY_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a random session key with default length
     * @return Randomly generated session key string
     */
    public static String generate() {
        return generate(DEFAULT_KEY_LENGTH);
    }

    /**
     * Generates a random session key with specified length
     * @param length Desired key length (must be positive)
     * @return Randomly generated session key string
     * @throws IllegalArgumentException if length ≤ 0
     */
    public static String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Key length must be positive");
        }

        return RANDOM.ints(length, 0, CHARACTER_POOL.length())
                .mapToObj(CHARACTER_POOL::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

}