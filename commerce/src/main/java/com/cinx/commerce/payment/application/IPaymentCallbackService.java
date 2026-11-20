package com.cinx.commerce.payment.application;

import com.cinx.commerce.payment.domain.PaymentProviderName;

public interface IPaymentCallbackService {
    /** Applies a payment notice whose signature the adapter has already verified. */
    SettlementOutcome settle(PaymentProviderName provider, ProviderPayment payment);
}
