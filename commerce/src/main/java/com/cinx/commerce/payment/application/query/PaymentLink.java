package com.cinx.commerce.payment.application.query;

import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentProviderName;

import java.time.LocalDateTime;

/** Where a learner pays an order with one gateway, and until when. */
public record PaymentLink(String paymentUrl, PaymentProviderName provider, LocalDateTime expiresAt) {

    public static PaymentLink of(PaymentAttempt attempt) {
        return new PaymentLink(attempt.getPaymentUrl(), attempt.getProvider(), attempt.getExpiresAt());
    }
}
