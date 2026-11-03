package com.cinx.commerce.shared.infrastructure.provider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;

public final class StripeSignatureVerifier {
    private final byte[] secret;
    private final Clock clock;
    private final long toleranceSeconds;

    public StripeSignatureVerifier(String secret, Clock clock, long toleranceSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret is required");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        this.toleranceSeconds = toleranceSeconds;
    }

    public void verify(byte[] rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Webhook signature is required");
        }
        long timestamp = -1;
        List<String> expectedSignatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if ("t".equals(pair[0])) timestamp = parseTimestamp(pair[1]);
            if ("v1".equals(pair[0]) && !pair[1].isBlank()) expectedSignatures.add(pair[1]);
        }
        if (timestamp < 0 || expectedSignatures.isEmpty()) {
            throw new IllegalArgumentException("Malformed webhook signature");
        }
        long now = Instant.now(clock).getEpochSecond();
        if (Math.abs(now - timestamp) > toleranceSeconds) {
            throw new IllegalArgumentException("Webhook timestamp is outside replay tolerance");
        }
        String signed = timestamp + "." + new String(rawBody, StandardCharsets.UTF_8);
        String actual = hex(hmac(signed.getBytes(StandardCharsets.UTF_8)));
        boolean matches = expectedSignatures.stream().anyMatch(expected ->
                MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8)));
        if (!matches) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed webhook timestamp", exception);
        }
    }

    private byte[] hmac(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify webhook signature", exception);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
