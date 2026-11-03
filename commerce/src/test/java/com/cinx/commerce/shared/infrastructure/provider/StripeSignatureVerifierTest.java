package com.cinx.commerce.shared.infrastructure.provider;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeSignatureVerifierTest {
    @Test
    void rejectsReplayAndTampering() throws Exception {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC);
        String body = "{\"id\":\"evt_1\"}";
        String secret = "whsec_test";
        String header = signature(secret, 1_700_000_000, body);
        StripeSignatureVerifier verifier = new StripeSignatureVerifier(secret, clock, 300);
        verifier.verify(body.getBytes(StandardCharsets.UTF_8), header);
        assertThatThrownBy(() -> verifier.verify("tampered".getBytes(StandardCharsets.UTF_8), header))
                .isInstanceOf(IllegalArgumentException.class);
        StripeSignatureVerifier stale = new StripeSignatureVerifier(secret, Clock.fixed(Instant.ofEpochSecond(1_700_001_000), ZoneOffset.UTC), 300);
        assertThatThrownBy(() -> stale.verify(body.getBytes(StandardCharsets.UTF_8), header))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String signature(String secret, long timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format("%02x", value));
        return "t=" + timestamp + ",v1=" + hex;
    }
}
