package com.cinx.commerce.payment.domain;

import java.time.LocalDateTime;
import java.util.Optional;

/** Port: where the payment links issued for orders are stored. */
public interface PaymentAttemptRepository {
    Optional<PaymentAttempt> findByProviderAndProviderReference(PaymentProviderName provider, String providerReference);

    /** The newest link for this order and gateway that a customer can still use. */
    Optional<PaymentAttempt> findLiveAttempt(String orderId, PaymentProviderName provider, LocalDateTime now);

    PaymentAttempt save(PaymentAttempt attempt);
}
