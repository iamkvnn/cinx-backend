package com.cinx.commerce.payment.domain;

/** Port: where the payments received from the gateways are stored. */
public interface PaymentRepository {
    /** Whether the gateway's event was already settled; each event counts once per provider. */
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);

    Payment save(Payment payment);
}
