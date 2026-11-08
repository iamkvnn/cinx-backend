package com.cinx.commerce.payment.application;

import com.cinx.commerce.payment.application.query.PaymentLink;
import com.cinx.commerce.payment.domain.PaymentProviderName;

public interface IOrderPaymentService {
    /** Issues, or reuses, a payment link for the caller's own pending order. */
    PaymentLink createPayment(String userId, String orderId, PaymentProviderName provider, String customerIp);
}
