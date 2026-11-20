package com.cinx.commerce.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payment link issued for an order. A link past {@code expiresAt} is never reused; a new attempt replaces it. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt {
    private String id;
    private String orderId;
    private PaymentProviderName provider;
    private String providerReference;
    private PaymentAttemptStatus status;
    private BigDecimal amount;
    private String paymentUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isCreated() {
        return status == PaymentAttemptStatus.CREATED;
    }

    public void markPaid(LocalDateTime now) {
        status = PaymentAttemptStatus.PAID;
        updatedAt = now;
    }

    public void markFailed(LocalDateTime now) {
        status = PaymentAttemptStatus.FAILED;
        updatedAt = now;
    }
}
