package com.cinx.commerce.payment.application;

import java.util.Map;
import java.util.Optional;

/**
 * Port: verifies a gateway's own signature on a payment notice and says what the notice reports. Each method throws
 * {@link IllegalArgumentException} for a notice that is not authentic, and returns nothing for an authentic one that
 * does not concern an attempt of ours.
 */
public interface PaymentNoticeReader {
    Optional<ProviderPayment> readStripe(byte[] rawBody, String signature);

    Optional<ProviderPayment> readMomo(Map<String, Object> ipn);

    Optional<ProviderPayment> readVnpay(Map<String, String> query);
}
