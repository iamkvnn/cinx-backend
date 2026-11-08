package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.PaymentNoticeReader;
import com.cinx.commerce.payment.application.ProviderPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class GatewayNoticeReader implements PaymentNoticeReader {
    private final StripeOrderPayments stripe;
    private final MomoOrderPayments momo;
    private final VnpayOrderPayments vnpay;

    @Override
    public Optional<ProviderPayment> readStripe(byte[] rawBody, String signature) {
        return stripe.parseWebhook(rawBody, signature);
    }

    @Override
    public Optional<ProviderPayment> readMomo(Map<String, Object> ipn) {
        return momo.parseIpn(ipn);
    }

    @Override
    public Optional<ProviderPayment> readVnpay(Map<String, String> query) {
        return vnpay.parseIpn(query);
    }
}
