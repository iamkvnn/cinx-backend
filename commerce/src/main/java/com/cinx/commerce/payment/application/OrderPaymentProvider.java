package com.cinx.commerce.payment.application;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** A gateway adapter that issues payment links for course orders. Domain services depend only on this. */
public interface OrderPaymentProvider {
    PaymentProviderName name();

    /** False while the gateway's keys are not configured; the order page then cannot offer it. */
    boolean available();

    /** How long a link stays payable. */
    Duration lifetime();

    /** Whether the gateway can take this amount (each has its own floor and ceiling). */
    boolean accepts(BigDecimal amount);

    CreatedPayment create(PaymentRequest request);

    /**
     * @param reference our own id for the attempt; the gateway echoes it back and the webhook finds the order by it
     * @param expiresAt when the link stops working, in business time
     * @param customerIp the payer's address, which VNPay asks for
     */
    record PaymentRequest(String reference, String orderId, BigDecimal amount, LocalDateTime expiresAt, String customerIp) {}

    record CreatedPayment(String paymentUrl) {}
}
