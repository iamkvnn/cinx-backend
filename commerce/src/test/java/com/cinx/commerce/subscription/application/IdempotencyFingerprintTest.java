package com.cinx.commerce.subscription.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyFingerprintTest {
    @Test
    void isStableAndScopedToUserAndCommand() {
        String first = IdempotencyFingerprint.sha256("u-1", "CHANGE", "offer=one");
        assertThat(first).isEqualTo(IdempotencyFingerprint.sha256("u-1", "CHANGE", "offer=one"));
        assertThat(first).isNotEqualTo(IdempotencyFingerprint.sha256("u-2", "CHANGE", "offer=one"));
        assertThat(first).isNotEqualTo(IdempotencyFingerprint.sha256("u-1", "CANCEL", "offer=one"));
    }
}
