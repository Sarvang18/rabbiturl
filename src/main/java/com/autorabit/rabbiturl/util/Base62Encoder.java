package com.autorabit.rabbiturl.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class Base62Encoder {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Base62Encoder() {
        // Utility class — no instantiation
    }

    /**
     * Converts the UUID's most significant bits to a Base62 string of exactly 7 characters.
     * Pads with 'a' on the left if the result is shorter than 7 characters.
     */
    public static String encode(UUID uuid) {
        long value = Math.abs(uuid.getMostSignificantBits());
        StringBuilder sb = new StringBuilder();

        while (value > 0 && sb.length() < LENGTH) {
            sb.append(ALPHABET.charAt((int) (value % 62)));
            value /= 62;
        }

        // Pad with 'a' if shorter than LENGTH
        while (sb.length() < LENGTH) {
            sb.append('a');
        }

        return sb.reverse().toString();
    }

    /**
     * Generates a random 7-character Base62 string using SecureRandom.
     */
    public static String generateRandom() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
