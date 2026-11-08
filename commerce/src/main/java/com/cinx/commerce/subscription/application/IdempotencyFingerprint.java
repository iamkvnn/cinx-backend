package com.cinx.commerce.subscription.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class IdempotencyFingerprint {
    private IdempotencyFingerprint() {
    }

    public static String sha256(String userId, String commandType, String normalizedRequest) {
        if (userId == null || userId.isBlank() || commandType == null || commandType.isBlank()
                || normalizedRequest == null) {
            throw new IllegalArgumentException("Idempotency fingerprint inputs are required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((userId + "\\n" + commandType + "\\n" + normalizedRequest)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
